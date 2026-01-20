package com.socialapp.service;

import com.socialapp.dto.ReactionCountsResponse;
import com.socialapp.dto.ReactionResponse;
import com.socialapp.model.Post;
import com.socialapp.model.PostReaction;
import com.socialapp.model.User;
import com.socialapp.repository.FriendRequestRepository;
import com.socialapp.repository.PostReactionRepository;
import com.socialapp.repository.PostRepository;
import com.socialapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReactionService {

    @Autowired
    private PostReactionRepository reactionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Add or update reaction on a post
     * - If no reaction exists, add new reaction
     * - If same reaction exists, remove it (toggle off)
     * - If different reaction exists, update to new reaction type
     * Only friends can react to posts
     */
    public ReactionResponse addOrUpdateReaction(Long postId, Long userId, PostReaction.ReactionType reactionType) {
       ...
        if (!isOwnPost) { // first bump (chunk of nested conditional)
            boolean areFriends = friendRequestRepository.areFriends(userId, post.getAuthor().getId());
            if (!areFriends) {
                throw new RuntimeException("You can only react to posts from your friends");
            }
        }
       ...
        if (existingReaction.isPresent()) { // Second bump (chunk of nested conditional)
            PostReaction reaction = existingReaction.get();
            if (reaction.getReactionType() == reactionType) {
                reactionRepository.delete(reaction);
                return createReactionResponse(null, postId, userId, null);
            } else {
                ...
                return createReactionResponse(reaction, postId, userId, reactionType);
            }
        } else {
            ...
            return createReactionResponse(reaction, postId, userId, reactionType);
        }
    }

    /**
     * Remove reaction from a post
     */
    public void removeReaction(Long postId, Long userId) {
        PostReaction reaction = reactionRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new RuntimeException("Reaction not found"));
        
        reactionRepository.delete(reaction);
    }

    /**
     * Get reaction counts by type for a post
     */
    public ReactionCountsResponse getReactionCounts(Long postId) {
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }

        // Get counts grouped by reaction type
        List<Object[]> counts = reactionRepository.getReactionCountsByType(postId);
        
        Map<String, Long> countsMap = new HashMap<>();
        // Initialize all reaction types with 0
        for (PostReaction.ReactionType type : PostReaction.ReactionType.values()) {
            countsMap.put(type.name(), 0L);
        }
        
        // Update with actual counts
        for (Object[] result : counts) {
            PostReaction.ReactionType type = (PostReaction.ReactionType) result[0];
            Long count = (Long) result[1];
            countsMap.put(type.name(), count);
        }

        ReactionCountsResponse response = new ReactionCountsResponse();
        response.setPostId(postId);
        response.setCounts(countsMap);
        return response;
    }

    /**
     * Get user's reaction to a post
     */
    public String getUserReaction(Long postId, Long userId) {
        Optional<PostReaction> reaction = reactionRepository.findByPostIdAndUserId(postId, userId);
        return reaction.map(r -> r.getReactionType().name()).orElse(null);
    }

    /**
     * Create ReactionResponse DTO
     */
    private ReactionResponse createReactionResponse(PostReaction reaction, Long postId, Long userId, PostReaction.ReactionType reactionType) {
        ReactionResponse response = new ReactionResponse();
        if (reaction != null) {
            response.setId(reaction.getId());
            response.setCreatedAt(reaction.getCreatedAt() != null ? reaction.getCreatedAt().format(DATE_FORMATTER) : null);
            response.setUpdatedAt(reaction.getUpdatedAt() != null ? reaction.getUpdatedAt().format(DATE_FORMATTER) : null);
        }
        response.setPostId(postId);
        response.setUserId(userId);
        response.setReactionType(reactionType != null ? reactionType.name() : null);
        
        // Get user details
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            response.setUserEmail(user.getEmail());
            response.setUserFullName(user.getFullName());
        }
        
        return response;
    }
}


