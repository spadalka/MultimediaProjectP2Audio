package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static javafx.scene.input.KeyCode.T;

public class Controller {
    final FileChooser fileChooser = new FileChooser();
    private Stage stage;
    private Scene scene;
    private Parent root;

    private int totalFramesRead = 0;

    @FXML
    private Label originalFileSizeLabel;
    @FXML
    private Label compressedFileSizeLabel;
    @FXML
    private Label compressionRateLabel;
    @FXML
    private Button compressButton;

    private short[] resultantByteArray;
    private float originalFileSize;

    public void openFileAudio(ActionEvent event) throws IOException {
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            originalFileSizeLabel.setText(String.format("%.2f", (float) file.length()/1024) + " KB");
            originalFileSize = file.length();
            resultantByteArray = extractAudioData(file);
            compressButton.setDisable(false);
        }
    }

    public void initiateCompression(ActionEvent event) throws IOException, DataFormatException {
        compress(resultantByteArray);
    }


    private short[] extractAudioData(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                bytesPerFrame = 1;
            }
            int numChannels = audioInputStream.getFormat().getChannels();
            int numBytes = (int) file.length();
            byte[] audioBytes = new byte[numBytes];
            short[] intervalDiffs = new short[numBytes/2];
            try {
                int numBytesRead = 0;
                int numFramesRead = 0;
                int x = 0;
                int y = 0;
                // Try to read numBytes bytes from the file.
                while (x < numBytes) {
                    numBytesRead = audioInputStream.read(audioBytes);
                    // Calculate the number of frames actually read.
                    numFramesRead = numBytesRead / bytesPerFrame;
                    totalFramesRead += numFramesRead;

                    if (numChannels == 1) {
                        if (x == 0) {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitude1 = bb.getShort();
                            intervalDiffs[0] = audioAmplitude1;
                            System.out.println(audioAmplitude1);

                        } else {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x - 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitude1 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitude2 = bb.getShort();

                            short difference = (short) (audioAmplitude2 - audioAmplitude1);
                            intervalDiffs[y] = difference;

                            if (x < 50) {
                                System.out.println(audioAmplitude1 + " " + audioAmplitude2);
                            }
                        }
                        x += 2;
                        y++;
                    } else if (numChannels == 2) {
                        if (x == 0) {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeLeft = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x + 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeRight = bb.getShort();

                            intervalDiffs[0] = (short) ((audioAmplitudeLeft+audioAmplitudeRight)/2); // mid channel
                            intervalDiffs[1] = (short) ((audioAmplitudeLeft-audioAmplitudeRight)/2); // side channel
                            System.out.println(audioAmplitudeLeft);
                            System.out.println(audioAmplitudeRight);


                        } else {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x - 4, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeLeft1 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x - 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeRight1 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeLeft2 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x + 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeRight2 = bb.getShort();

                            if (x < 50) {
                                System.out.println(audioAmplitudeLeft1 + " " + audioAmplitudeLeft2);
                                System.out.println(audioAmplitudeRight1 + " " + audioAmplitudeRight2);
                            }

                            short diffChannel1 = (short) (audioAmplitudeLeft2 - audioAmplitudeLeft1);
                            short diffChannel2 = (short) (audioAmplitudeRight2 - audioAmplitudeRight1);


                            intervalDiffs[y] = (short) ((diffChannel1+diffChannel2)/2); // mid channel
                            intervalDiffs[y+1] = (short) ((diffChannel1-diffChannel2)/2); // side channel
                        }
                        x += 4;
                        y += 2;
                    }
                }
                return intervalDiffs;

            } catch (Exception ex) {
                System.out.println("no1");
                return null;
            }
        } catch (Exception e) {
            System.out.println("no2");
            return null;
        }
    }

    public static<Byte> byte[] subArray(byte[] array, int beg, int end) {
        return Arrays.copyOfRange(array, beg, end + 1);
    }

    public static<Short> short[] subArray(short[] array, int beg, int end) {
        return Arrays.copyOfRange(array, beg, end + 1);
    }

    public void compress(short[] shortArray) throws DataFormatException, IOException {
//        short[] shortArr = {23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21, 23, 34, 423, 43, 21};
//        // https://stackoverflow.com/questions/2984538/how-to-use-bytearrayoutputstream-and-dataoutputstream-simultaneously-in-java
        short[] subShort = subArray(shortArray, 0, 50);
        System.out.println(Arrays.toString(subShort));


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (short value : shortArray) {
            dos.writeShort(value);
        }
        byte[] bytes = baos.toByteArray();

        byte[] subarray = subArray(bytes, 0, 50);
        System.out.println(Arrays.toString(subarray));

        File originalAudio = new File("original");
        // https://stackoverflow.com/questions/4350084/byte-to-file-in-java/4350109
        try (FileOutputStream stream = new FileOutputStream(originalAudio)) {
            stream.write(bytes);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] output = new byte[bytes.length];
        Deflater compressor = new Deflater();
        compressor.setInput(bytes);
        compressor.finish();
        int compressedDataLength = compressor.deflate(output);
//        System.out.println(compressedDataLength);
        compressor.end();
        byte[] finalOutput = new byte[compressedDataLength];
        System.arraycopy(output, 0, finalOutput, 0, compressedDataLength );

        byte[] subarray2 = subArray(finalOutput, 0, 50);
        System.out.println(Arrays.toString(subarray2));

        File compressedAudioFile = new File("compressedAudio");
        // https://stackoverflow.com/questions/4350084/byte-to-file-in-java/4350109
        try (FileOutputStream stream = new FileOutputStream(compressedAudioFile)) {
            stream.write(finalOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
        compressedFileSizeLabel.setText(String.format("%.2f", (float) (compressedAudioFile.length()/1024)) + " KB");
        compressionRateLabel.setText(String.valueOf(originalFileSize/compressedAudioFile.length()));
//        Inflater decompresser = new Inflater();
//        decompresser.setInput(output, 0, compressedDataLength);
//        byte[] result = new byte[100];
//        int resultLength = decompresser.inflate(result);
//        decompresser.end();
//        System.out.println(Arrays.toString(result));





    }

}
