package com.x.backend.services.image;

import com.x.backend.exceptions.ImageNotFoundException;
import com.x.backend.exceptions.UnableToDownloadFileException;
import com.x.backend.exceptions.UnableToUploadFileException;
import com.x.backend.models.Image;
import com.x.backend.repositories.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;

    public ImageServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    private static final File IMAGE_BUCKET_DIRECTORY = new File("/home/capellax/projects/JavaProjects/X/backend/img");
    private static final String IMAGE_ENDPOINT_URL = "http://localhost:8888/image/";
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "image/gif");

    @Override
    public Image uploadImage(MultipartFile file, String prefix) {
        try {
            // Mime type validation
            if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
                throw new IllegalArgumentException("Invalid file type. Only JPG, PNG, and GIF are allowed.");
            }

            // Determine the file extension
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "." + Objects.requireNonNull(file.getContentType()).split("/")[1];

            // Create a unique file name
            String imageName = prefix + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path destinationPath = IMAGE_BUCKET_DIRECTORY.toPath().resolve(imageName);

            // Save the file in the provided path
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // Construct the image object to be saved in the database
            String imageUrl = IMAGE_ENDPOINT_URL + imageName;
            Image image = new Image(imageName, file.getContentType(), destinationPath.toString(), imageUrl);
            return imageRepository.save(image);

        } catch (IOException e) {
            throw new UnableToUploadFileException();
        }
    }

    private Image getImageByImageName(String imageName) {
        return imageRepository.findByImageName(imageName)
                .orElseThrow(ImageNotFoundException::new);
    }

    @Override
    public byte[] downloadImage(String imageName) {
        try {
            Image image = getImageByImageName(imageName);
            String imagePath = image.getImagePath();
            return Files.readAllBytes(new File(imagePath).toPath());

        } catch (IOException e) {
            throw new UnableToDownloadFileException();
        }
    }

    @Override
    public String getImageType(String imageName) {
        Image image = getImageByImageName(imageName);
        return image.getImageType();
    }

}
