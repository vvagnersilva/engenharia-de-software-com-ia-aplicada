package br.dev.showcase.mcp.client.cli;

import java.util.ArrayList;
import java.util.List;

import br.dev.showcase.mcp.client.demo.AsyncDemoService;
import br.dev.showcase.mcp.client.demo.ShowcaseDemoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * CLI das demonstracoes.
 *
 * <pre>
 * java -jar showcase-client.jar --demo=tools
 * java -jar showcase-client.jar --demo=all
 * </pre>
 *
 * <p>Sem o argumento {@code --demo}, a aplicacao sobe normalmente e fica servindo
 * a API REST.
 */
@Component
public class DemoRunner implements ApplicationRunner {

    private final ShowcaseDemoService demos;
    private final AsyncDemoService asyncDemo;
    private final ApplicationContext context;

    public DemoRunner(ShowcaseDemoService demos, AsyncDemoService asyncDemo, ApplicationContext context) {
        this.demos = demos;
        this.asyncDemo = asyncDemo;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("demo")) {
            return;
        }

        List<String> requested = args.getOptionValues("demo");
        List<String> toRun = new ArrayList<>();
        if (requested.contains("all")) {
            toRun.addAll(demos.catalog().keySet());
            toRun.add("async");
        }
        else {
            toRun.addAll(requested);
        }

        int failures = 0;
        for (String demo : toRun) {
            try {
                String report = "async".equals(demo) ? asyncDemo.run().asText() : demos.run(demo).asText();
                System.out.println();
                System.out.println(report);
            }
            catch (RuntimeException ex) {
                failures++;
                System.out.println();
                System.out.println("== " + demo + " FALHOU ==");
                System.out.println(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        System.out.println();
        System.out.println("Demos executadas: " + toRun.size() + ", falhas: " + failures);

        int exitCode = failures == 0 ? 0 : 1;
        SpringApplication.exit(context, () -> exitCode);
    }
}
