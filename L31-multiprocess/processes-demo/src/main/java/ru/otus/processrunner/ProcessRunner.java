package ru.otus.processrunner;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private static final String BASE_PATH = "./L31-multiprocess/processes-demo";
    private static final String FILES_PATH = BASE_PATH + "/files";
    private static final String SRC_PATH = BASE_PATH + "/src/main/java";

    private static final String JAVA_CMD = "java";
    private static final String JAVAC_CMD = "javac";

    private static final String JOBS_PACKAGE_DIR = "/ru/otus/processrunner/jobs/";
    private static final String JOB_CLASS = "ru.otus.processrunner.jobs.Job";
    private static final String JOB_CLASS_FILE_NAME = "Job.java";

    public static void main(String[] args) throws Exception {
        //compileJobClass();

        //simpleJobExecution();
        //jobExecutionWithOutputInterception();
        //compareTwoFilesAsynchronouslyWithAnExternalTool();
        //printProcessesList();
    }

    private static void simpleJobExecution() throws Exception {
        System.out.println("begin");

        var currentDir = new File(SRC_PATH);
        new ProcessBuilder(JAVA_CMD, JOB_CLASS)
                //.inheritIO()
                .directory(currentDir)
                .start();

        System.out.println("end");

    }

    private static void jobExecutionWithOutputInterception() throws Exception {
        System.out.println("begin\n");
        var currentDir = new File(SRC_PATH);

        var processBuilder = new ProcessBuilder(JAVA_CMD, JOB_CLASS)
                .directory(currentDir);

        Map<String, String> environment = processBuilder.environment();
        environment.put("endOfRange", "3");

        var process = processBuilder.start();

        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(String.format("process out: %s", line));
            }
        }

        System.out.println("\nwaiting for process...");
        process.waitFor(1, TimeUnit.MINUTES);

        System.out.println("end");
    }

    private static void compareTwoFilesAsynchronouslyWithAnExternalTool() throws Exception {
        System.out.println("begin\n");

        boolean isWindows =  System.getProperty("os.name").toLowerCase().contains("windows");
        ProcessBuilder processBuilder = isWindows
                ? new ProcessBuilder("fc", "/N", "file1.txt", "file2.txt")
                : new ProcessBuilder("cmp", "file1.txt", "file2.txt");

        System.out.println("starting process...\n");
        Process process = processBuilder
                .directory(new File(FILES_PATH))
                .inheritIO()
                .start();

        CompletableFuture<Process> compareResult = process.onExit();
        System.out.println("next action 1...");
        System.out.println("next action 2...");
        System.out.println("next action 3...");

        compareResult.thenApply(p -> {
            System.out.println(
                    String.format("\ncomparison result: %s",
                            (p.exitValue() == 0) ? "files equal" : "files NOT equal"));
            return true;
        });

        Thread.sleep(1000);
        System.out.println("\nend");
    }

    public static void printProcessesList() {
        ProcessHandle.allProcesses()
                .forEach(process -> {
                    String info = String.format("%8d %s",
                            process.pid(),
                            process.info().command().orElse("-"));
                    System.out.println(info);
                });
    }

    private static void compileJobClass() throws Exception {
        new ProcessBuilder(JAVAC_CMD, JOB_CLASS_FILE_NAME).directory(new File(SRC_PATH + JOBS_PACKAGE_DIR))
                .start().waitFor(1, TimeUnit.MINUTES);
    }

}