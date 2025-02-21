package com.x.backend.services.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.x.backend.dto.CreatePostRequest;
import com.x.backend.exceptions.PostNotFoundException;
import com.x.backend.exceptions.UnableToCreatePostException;
import com.x.backend.models.ApplicationUser;
import com.x.backend.models.Image;
import com.x.backend.models.Post;
import com.x.backend.repositories.PostRepository;
import com.x.backend.services.image.ImageService;
import com.x.backend.services.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final ImageService imageService;

    public PostServiceImpl(PostRepository postRepository, ObjectMapper objectMapper, UserService userService, ImageService imageService) {
        this.postRepository = postRepository;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.imageService = imageService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Post createPost(CreatePostRequest createPostRequest) {
        Post post = new Post();
        try {
            post.setPostedDate(
                    createPostRequest.scheduled() ?
                            createPostRequest.scheduledDate() :
                            LocalDateTime.now()
            );
            post.setContent(createPostRequest.content());
            post.setAuthor(createPostRequest.author());
            post.setScheduled(createPostRequest.scheduled());
            post.setScheduledDate(createPostRequest.scheduledDate());
            post.setAudience(createPostRequest.audience());
            post.setReplyRestriction(createPostRequest.replyRestriction());
            return postRepository.save(post);
        }
        catch (Exception e) {
            throw new UnableToCreatePostException();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Post createMediaPost(String postJson, List<MultipartFile> files) {
        objectMapper.registerModule(new JavaTimeModule());
        try {
            // Convert JSON into CreatePostRequest DTO
            CreatePostRequest createPostRequest = objectMapper.readValue(postJson, CreatePostRequest.class);
            // Construct a brand-new Post object
            Post post = new Post();
            post.setContent(createPostRequest.content());
            // Determine the mentioned users in the post
            List<ApplicationUser> mentionedUsers = extractMentionedUsers(createPostRequest.content());
            // Determine the timing considering the scheduling state
            if (Boolean.TRUE.equals(createPostRequest.scheduled())) {
                post.setPostedDate(createPostRequest.scheduledDate());
            }
            else {
                post.setPostedDate(LocalDateTime.now());
            }
            // Set the other object fields
            post.setAuthor(createPostRequest.author());
            post.setReplies(createPostRequest.replies());
            post.setScheduled(createPostRequest.scheduled());
            post.setScheduledDate(createPostRequest.scheduledDate());
            post.setAudience(createPostRequest.audience());
            post.setReplyRestriction(createPostRequest.replyRestriction());
            // Upload the post images
            List<Image> postImages = new ArrayList<>();
            for (MultipartFile file : files) {
                Image uploadedImage = imageService.uploadImage(file, "post_image");
                postImages.add(uploadedImage);
            }
            post.setImages(postImages);
            // TODO: Send notification to the mentioned users
            // Save the post
            return postRepository.save(post);
        }
        catch (Exception e) {
            throw new UnableToCreatePostException();
        }
    }

    private List<ApplicationUser> extractMentionedUsers(String postContent) {
        List<ApplicationUser> mentionedUsers = new ArrayList<>();
        if (postContent != null || postContent.trim().isEmpty()) {
            return mentionedUsers;
        }
        String[] words = postContent.split(",");
        for (String word : words) {
            if (word.startsWith("@")) {
                String username = word.substring(1).replaceAll("[^a-zA-Z0-9]*$", "");
                ApplicationUser mentionedUser = userService.getUserByUsername(username);
                if (mentionedUser != null) {
                    mentionedUsers.add(mentionedUser);
                }
            }
        }
        return mentionedUsers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        return postRepository.findAllWithRelations();
    }

    @Override
    @Transactional(readOnly = true)
    public Post getPostById(Integer id) {
        return postRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getAllPostsByAuthor(ApplicationUser author) {
        Set<Post> userPosts = postRepository.findByAuthor(author);
        List<Post> sortedPosts = new ArrayList<>(userPosts);
        sortedPosts.sort(Comparator.comparing(Post::getPostedDate).reversed());
        return sortedPosts;
    }

    @Override
    public void deletePost(Integer id) {
        Post post = getPostById(id);
        postRepository.delete(post);
    }

}
