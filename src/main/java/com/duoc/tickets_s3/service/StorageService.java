
package com.duoc.tickets_s3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${file.storage-dir}")
    private String efsStorageDir;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Guarda temporalmente el PDF en EFS
    public File guardarEnEfs(
            String nombreArchivo,
            byte[] contenidoBytes) throws IOException {

        Path directorio = Paths.get(efsStorageDir);

        Files.createDirectories(directorio);

        Path rutaArchivo = directorio.resolve(nombreArchivo);

        Files.write(rutaArchivo, contenidoBytes);

        return rutaArchivo.toFile();
    }

    // Recupera un PDF almacenado temporalmente en EFS
    public File obtenerArchivoEfs(String nombreArchivo) {
        Path rutaArchivo = Paths.get(efsStorageDir)
                .resolve(nombreArchivo)
                .normalize();

        return rutaArchivo.toFile();
    }

    // Sube el PDF desde EFS hacia Amazon S3
    public String subirAS3(
            String eventoId,
            String usuarioId,
            String nombreArchivo,
            File archivo) {

        String s3Key = eventoId + "/" + usuarioId + "/" + nombreArchivo;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putRequest, archivo.toPath());

        return "s3://" + bucketName + "/" + s3Key;
    }

    // Descarga un PDF desde Amazon S3
    public byte[] descargarDesdeS3(
            String eventoId,
            String usuarioId,
            String nombreArchivo) {

        String s3Key = eventoId + "/" + usuarioId + "/" + nombreArchivo;

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        return s3Client.getObjectAsBytes(getRequest).asByteArray();
    }

    // Eliminar un PDF almacenado en EFS
    public void eliminarArchivoEfs(String nombreArchivo) throws IOException {

        Path rutaArchivo = Paths.get(efsStorageDir)
                .resolve(nombreArchivo)
                .normalize();

        Files.deleteIfExists(rutaArchivo);
    }

    // Eliminar un PDF almacenado en Amazon S3
    public void eliminarDeS3(
            String eventoId,
            String usuarioId,
            String nombreArchivo) {

        String s3Key = eventoId + "/" + usuarioId + "/" + nombreArchivo;

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}
