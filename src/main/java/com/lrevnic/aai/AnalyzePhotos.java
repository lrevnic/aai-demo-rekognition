package com.lrevnic.aai;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesRequest;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesResponse;
import software.amazon.awssdk.services.rekognition.model.Celebrity;

public class AnalyzePhotos {

 // Returns a list of WorkItem objects that contains labels.
 public ArrayList<WorkItem> detectLabels(byte[] bytes, String key) {

    Region region = Region.US_WEST_2;
    RekognitionClient rekClient = RekognitionClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(region)
            .build();

    try {

        SdkBytes sourceBytes = SdkBytes.fromByteArray(bytes);

        // Create an Image object for the source image.
        Image souImage = Image.builder()
                .bytes(sourceBytes)
                .build();

        DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder()
                .image(souImage)
                .maxLabels(10)
                .build();

        DetectLabelsResponse labelsResponse = rekClient.detectLabels(detectLabelsRequest);

        // Write the results to a WorkItem instance.
        List<Label> labels = labelsResponse.labels();
        ArrayList<WorkItem> list = new ArrayList<>();
        WorkItem item ;
        for (Label label: labels) {
            item = new WorkItem();
            item.setKey(key); // identifies the photo.
            item.setConfidence(label.confidence().toString());
            item.setName(label.name());
            list.add(item);
        }
        return list;

    } catch (RekognitionException e) {
        System.out.println(e.getMessage());
        System.exit(1);
    }
    return null ;
  }

  public ArrayList<WorkItem> recognize(byte[] bytes, String key) {

    Region region = Region.US_WEST_2;
    RekognitionClient rekClient = RekognitionClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(region)
            .build();

    // Write the results to a WorkItem instance.
    ArrayList<WorkItem> list = new ArrayList<>();
            
    try {
         SdkBytes sourceBytes = SdkBytes.fromByteArray(bytes);
         Image souImage = Image.builder()
             .bytes(sourceBytes)
             .build();

         RecognizeCelebritiesRequest request = RecognizeCelebritiesRequest.builder()
             .image(souImage)
             .build();

        RecognizeCelebritiesResponse result = rekClient.recognizeCelebrities(request) ;
         
        WorkItem item ;

        List<Celebrity> celebs=result.celebrityFaces();
        System.out.println(celebs.size() + " celebrity(s) were recognized.\n");
        System.out.println(result.unrecognizedFaces().size() + " face(s) were unrecognized.");
       
        DynamoDBService ddb = new DynamoDBService();
        String tableName = "celebrities";
        String keyName = "s3object";
        ddb.createTable(tableName, keyName);
        
        for (Celebrity celebrity: celebs) {
            ddb.putItemInTable(tableName, keyName, key, celebrity.name());
        }

     } catch (RekognitionException e) {
         System.out.println(e.getMessage());
     }
     
     return list;
 }
 
 public String anonimize() {
        DynamoDBService ddb = new DynamoDBService();
        String tableName = "celebrities";
        ddb.deleteTable(tableName);
        return "";
 }
   
  // snippet-start:[rekognition.java2.recognize_celebs.main]
 public void recognizeAllCelebrities(RekognitionClient rekClient, String sourceImage) {

     try {
         InputStream sourceStream = new FileInputStream(sourceImage);
         SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);
         Image souImage = Image.builder()
             .bytes(sourceBytes)
             .build();

         RecognizeCelebritiesRequest request = RecognizeCelebritiesRequest.builder()
             .image(souImage)
             .build();

         RecognizeCelebritiesResponse result = rekClient.recognizeCelebrities(request) ;
         List<Celebrity> celebs=result.celebrityFaces();
         System.out.println(celebs.size() + " celebrity(s) were recognized.\n");
         for (Celebrity celebrity: celebs) {
             System.out.println("Celebrity recognized: " + celebrity.name());
             System.out.println("Celebrity ID: " + celebrity.id());

             System.out.println("Further information (if available):");
             for (String url: celebrity.urls()){
                 System.out.println(url);
             }
             System.out.println();
         }
         System.out.println(result.unrecognizedFaces().size() + " face(s) were unrecognized.");

     } catch (RekognitionException | FileNotFoundException e) {
         System.out.println(e.getMessage());
         System.exit(1);
     }
 }
 
}