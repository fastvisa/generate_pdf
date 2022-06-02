package com.fastvisa.manipulatepdf;

import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.fastvisa.services.AwsS3Service;
import com.fastvisa.services.FormService;
import com.fastvisa.services.ReceiptService;

@RestController
public class FormsController {
  private String url_download;

  @PostMapping(path = "/api/v1/fillform", consumes = "application/json", produces = "application/json")
  public Form fillform(@RequestBody String bodyParameter) throws Exception {
    Gson gson = new Gson();
    JSONParser jsonParser = new JSONParser();
    JSONArray form_array = new JSONArray();
    Object form_object = new Object();
    FormService formService = new FormService();

    Form g = gson.fromJson(bodyParameter, Form.class);

    Object form_data = g.formData();
    String template_path = g.templatePath();
    String output_name = g.outputName();

    if( form_data instanceof String ) {
      FileReader form_reader = new FileReader((String) form_data);
      form_object = jsonParser.parse(form_reader);
      form_array = (JSONArray) form_object;
    } else {
      form_object = jsonParser.parse(gson.toJson(form_data));
      form_array = (JSONArray) form_object;
    }

    File file = File.createTempFile(output_name, "pdf");

    formService.fillForm(form_array, template_path, file, output_name);

    uploadS3(file, output_name);

    return new Form(form_data, template_path, output_name, url_download, "success");
  }


  @PostMapping(path = "/api/v1/generate-receipt", consumes = "application/json", produces = "application/json")
  public Receipt generateReceipt(@RequestBody String bodyParameter) throws Exception {
    Gson gson = new Gson();
    JSONParser jsonParser = new JSONParser();
    // JSONArray form_array = new JSONArray();
    Object form_object = new Object();
    ReceiptService receiptService = new ReceiptService();

    Receipt g = gson.fromJson(bodyParameter, Receipt.class);

    System.out.println(bodyParameter);
    System.out.println(g);
    System.out.println(g.getForm_data());

    Object form_data = g.getForm_data();
    // String template_path = g.getTemplate_path();
    String output_name = g.getOutput_name();

    if( form_data instanceof String ) {
      System.out.println("puriring");
      FileReader form_reader = new FileReader((String) form_data);
      form_object = jsonParser.parse(form_reader);
      // form_array = (JSONArray) form_object;
    } else {
      System.out.println("balalala");
      form_object = jsonParser.parse(gson.toJson(form_data));
      // form_array = (JSONArray) form_object;
    }

    System.out.println(form_object);

    File file = File.createTempFile(output_name, ".pdf");
    FileOutputStream fileOutputStream = new FileOutputStream(file);

    receiptService.generateInvoice(g, fileOutputStream);
    uploadS3(file, output_name);

    return new Receipt(form_data, output_name, url_download, "success");
  }



  @Value("${aws.accessKey}")
  private String accessKey;
  @Value("${aws.secretKey}")
  private String secretKey;
  @Value("${aws.s3bucket.name}")
  private String bucketName;

  private void uploadS3(File file, String output_name) {
    try {
      AwsS3Service s3Service = new AwsS3Service(accessKey, secretKey);
      s3Service.postObject(bucketName, output_name + ".pdf", file);
      String url_download = s3Service.getUrl(bucketName, output_name + ".pdf");

      file.delete();

      this.url_download = url_download;
    } catch (AmazonServiceException e) {
      e.printStackTrace();
    } catch (SdkClientException e) {
      e.printStackTrace();
    }
  }

}