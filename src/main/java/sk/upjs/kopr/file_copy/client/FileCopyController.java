package sk.upjs.kopr.file_copy.client;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.client.FileInfoReceiver;
import sk.upjs.kopr.file_copy.client.FileReceiveTask;
import sk.upjs.kopr.file_copy.server.Server;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class FileCopyController extends Service<Void> {

    private final ProgressBar copyProgressBar;
    private final int threadCount;

    private final List<Future<String>> futures = new ArrayList<>();
    private AtomicLong savedDataProgress = new AtomicLong(0);
    private File oSubore = new File("oSubore");
    private boolean newData = false;
    private long fileSize;
    private File fileToCopy;
    private ExecutorService executor;
    public CountDownLatch cdl;

    public FileCopyController(ProgressBar copyProgressBar, int threadCount) {
        this.copyProgressBar = copyProgressBar;
        this.threadCount = threadCount;
        cdl= new CountDownLatch(threadCount);
        this.executor = Executors.newFixedThreadPool(threadCount);
        FileInfo fileInfo = FileInfoReceiver.getLocalhostServerFileInfo();
        if (fileInfo == null) {
            showError("Nepodarilo sa získať informácie o súbore zo servera.");
            return;
        }
        String originalFileName = fileInfo.getFileName();
        String extension = getExtension(originalFileName);
        fileToCopy = new File("C:\\Users\\marti\\novysubor1" + extension);
        fileSize = fileInfo.getFileSize();
    }

    public void startCopy() {
        try {
            long blockSize = fileSize / threadCount;

            for (int i = 0; i < threadCount; i++) {
                long offset = i * blockSize;
                long length = (i == threadCount - 1) ? (fileSize - offset) : blockSize;

                FileReceiveTask task = new FileReceiveTask(fileToCopy, fileSize, offset, length, InetAddress.getByName("localhost"), Server.SERVER_PORT, this);
                Future<String> future = executor.submit(task);
                futures.add(future);
            }


            cdl.await();
            copyProgressBar.setProgress(1.0);
            showInfo("Kopírovanie bolo úspešne dokončené.");
        }  catch (InterruptedException e){


        } catch (Exception e) {
            e.printStackTrace();
            showError("Počas kopírovania nastala chyba: " + e.getMessage());
        }
    }

    public void processFutures() {

        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null) {
                    saveDataToFile(result);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                showError("Chyba pri spracovaní Future: " + e.getMessage());
            }

        }
        if(savedDataProgress.get()==fileSize){

            System.out.println("Celý súbor bol poslaný");
            oSubore.deleteOnExit();

        }else{

            saveDataToFile("priebeh" + " " + savedDataProgress.toString());
        }
        if (!executor.isShutdown()) {
            executor.shutdown();
        }



    }

    private void downloadContinue() {
        executor = Executors.newFixedThreadPool(threadCount);
        BufferedReader br;
        FileReader fr;
        try {
            fr = new FileReader(oSubore);
            br = new BufferedReader(fr);
            String info = br.readLine();
            FileReceiveTask task;
            while (info != null) {
                if (info.contains("priebeh")) {
                    savedDataProgress.set(Long.parseLong(info.split(" ")[1]));

                } else {
                    if (!info.contains("vlakna")) {
                        String[] splitted = info.split(" ");
                        task = new FileReceiveTask(fileToCopy, fileSize, Long.parseLong(splitted[0]),
                                Long.parseLong(splitted[1]), InetAddress.getByName("localhost"), Server.SERVER_PORT, this);
                        futures.add(executor.submit(task));
                    }
                }
                info = br.readLine();
            }
            br.close();
            oSubore.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void saveDataToFile(String returnedFuture) {
        if (!returnedFuture.isEmpty()) {
            try (FileWriter fw = new FileWriter(oSubore, true)) {
                if (oSubore.length() == 0 || !oSubore.isFile()) {
                    newData = true;
                }
                if (newData) {
                    fw.write("vlakna " + threadCount + "\n");
                    newData = false;
                }
                fw.write(returnedFuture + " " + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected Task<Void> createTask() {

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {
                try {
                    executor = Executors.newFixedThreadPool(threadCount);
                    if(oSubore.createNewFile() || oSubore.length() == 0){
                        startCopy();
                        cdl.await();
                        savedDataProgress.set(fileSize);


                        if (savedDataProgress.get() == fileSize) {

                            System.out.println("Stahovanie dokoncene ");
                            oSubore.deleteOnExit();
                        }
                    }else{
                        downloadContinue();
                        cdl.await();
                        savedDataProgress.set(fileSize);
                        executor.shutdown();

                        if (savedDataProgress.get() == fileSize) {

                            System.out.println("Stahovanie dokoncene ");
                            oSubore.deleteOnExit();
                        }
                    }
                } catch (InterruptedException e ) {

                }catch(IOException e){
                    System.out.println("Problem so suborom");
                }
                return null;
            }

            @Override
            public void cancelled() {
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                    System.out.println("Stahovanie je prerusene");
                    processFutures();

                }

            }
        };
        return task;
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.showAndWait();
        });
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1 && dotIndex != 0) ? fileName.substring(dotIndex) : "";
    }
    public void addData(long data) {
        savedDataProgress.getAndAdd(data);

    }
}
