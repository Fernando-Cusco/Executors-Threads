package cusco.mejia.httpclientthreads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@SpringBootApplication
@RestController
public class HttpClientThreadsApplication {

    private static final String URL = "https://www.7timer.info/bin/astro.php?lon=113.2&lat=23.1&ac=0&unit=metric&output=json&tzshift=0";

    private static final Logger LOG = Logger.getLogger(HttpClientThreadsApplication.class.getName());

    static ExecutorService executorService = Executors.newCachedThreadPool();


    public static void main(String[] args) {
        SpringApplication.run(HttpClientThreadsApplication.class, args);

    }

    @GetMapping("/")
    public String enviar() {
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(100);
                CompletableFuture<HttpResponse<String>> response = new HttpClientThreadsApplication().enviarPetiones(i);
                LOG.info("Response: " + response.get().toString().substring(0,1) + ": peticion: " + i);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                if (executorService.isTerminated()) {
                    executorService.shutdownNow();
                    LOG.info("Finished");
                }
            }
        }
        return "Finished";
    }

    @GetMapping("/state")
    public String estado() {
        calcular();
        return executorService.isTerminated() ? "Terminado" : "No terminado";
    }

    public CompletableFuture<HttpResponse<String>> enviarPetiones(int peticion) {
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(URL))
                    .version(HttpClient.Version.HTTP_2)
                    .timeout(Duration.of(5, ChronoUnit.SECONDS))
                    .GET()
                    .build();
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            CompletableFuture<HttpResponse<String>> response = HttpClient
                    .newBuilder()
                    .executor(executorService)
                    .build()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(data -> {
                                LOG.info("Finalizo la peticion " + peticion);
                                return data;
                            })
                    .toCompletableFuture();
            // thread state
            return response;
            // finish the future
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            // thread state
            LOG.info("Thread state: " + Thread.currentThread().getState());

        }
    }

    public void calcular() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                LOG.info("Thread state: " + Thread.currentThread().getState());
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(100);
                        CompletableFuture<HttpResponse<String>> response = new HttpClientThreadsApplication().enviarPetiones(i);
                        LOG.info("Response: " + response.get().toString().substring(0,1) + ": peticion: " + i);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (executorService.isTerminated()) {
                            executorService.shutdownNow();
                            LOG.info("Finished");
                        }
                    }
                }
            }
        });
    }

}
