package com.lrevnic.aai;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Handler implements RequestHandler<Map<String,String>, String> {

@Override
public String handleRequest(Map<String, String> event, Context context) {
    LambdaLogger logger = context.getLogger();
    S3Service s3Service = new S3Service();
    AnalyzePhotos photos = new AnalyzePhotos();

    String bucketName = System.getenv("BUCKET_NAME");
    List<String> myKeys = s3Service.listBucketObjects(bucketName);

    String delFlag = event.get("flag");
    logger.log("FLAG IS: " + delFlag);
    if (delFlag.compareTo("true") == 0) {

        // Create a List to store the data.
        List<ArrayList<WorkItem>> myList = new ArrayList<>();

        // loop through each element in the List and tag the assets.
        for (String key : myKeys) {
            byte[] keyData = s3Service.getObjectBytes(bucketName, key);
            ArrayList<WorkItem> item = photos.detectLabels(keyData, key);
            myList.add(item);

            photos.recognize(keyData, key);
        }

        s3Service.tagAssets(myList, bucketName);
        logger.log("All Assets in the bucket are tagged!");

    } else {
        
        // Delete all object tags.
        for (String key : myKeys) {
            s3Service.deleteTagFromObject(bucketName, key);
            logger.log("All Assets in the bucket are deleted!");
        }
        
        photos.anonimize();
     }
    return delFlag;
  }
 }