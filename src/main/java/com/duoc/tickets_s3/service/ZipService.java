
package com.duoc.tickets_s3.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    public byte[] generarZip(Map<String, byte[]> archivos)
            throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(baos)) {

            for (Map.Entry<String, byte[]> archivo : archivos.entrySet()) {

                ZipEntry entrada = new ZipEntry(archivo.getKey());

                zip.putNextEntry(entrada);
                zip.write(archivo.getValue());
                zip.closeEntry();
            }

            zip.finish();

            return baos.toByteArray();
        }
    }
}
