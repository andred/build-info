package org.jfrog.build.extractor.clientConfiguration.util.spec;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;

import java.io.IOException;

/**
 * Consumer object to use with the ProducerConsumerExecutor during artifact deployment by FileSpec operation.
 *
 * Created by Bar Belity on 27/03/2018.
 */
public class SpecDeploymentConsumer extends ConsumerRunnableBase {

    private ProducerConsumerExecutor executor;
    private Log log;
    private ArtifactoryBuildInfoClient client;
    private static final int MAX_TRIES = 75;

    public SpecDeploymentConsumer(ArtifactoryBuildInfoClient client){
        this.client = client;
    }

    @Override
    public void consumerRun() {
        int tries;
        while (!Thread.interrupted()) {
            try {
                ProducerConsumerItem item = executor.take();

                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE DeployDetails, return it to the queue and exit
                    executor.put(item);
                    break;
                }
                tries = 0;
                while (true && !Thread.interrupted()) {
                    ++tries;
                    try {
                        // Perform artifact deploy
                        client.deployArtifact((DeployDetails) item,
                                              "[" + Thread.currentThread().getName()
                                              + "(try #" + Integer.toString (tries) + ")]");
                        break;
                    } catch (IOException e) {
                        if (tries == MAX_TRIES)
                            throw e;
                    }
                }
            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                // Throw unchecked exception for the UncaughtExceptionHandler
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setExecutor(ProducerConsumerExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }
}
