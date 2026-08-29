package com.sebu.backend.community.comment.service;

import com.sebu.backend.community.comment.domain.CommunityComment;
import com.sebu.backend.community.comment.dto.CommentListResponse;
import com.sebu.backend.community.comment.repository.CommunityCommentRepository;
import com.sebu.backend.community.common.CommunityAuthorMapper;
import com.sebu.backend.community.exception.InvalidPostQueryException;
import com.sebu.backend.community.exception.PostNotFoundException;
import com.sebu.backend.community.post.repository.CommunityPostRepository;
import com.sebu.backend.global.auth.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityCommentQueryService {
    private static final int MAX_PAGE_SIZE = 50;

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CommunityAuthorMapper authorMapper;

    @Transactional(readOnly = true)
    public CommentListResponse findComments(Long postId, int page, int size) {
        validatePage(page, size);
        if (!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
            throw new PostNotFoundException();
        }

        Long viewerId = currentUserProvider.currentUserId().orElse(null);
        Page<CommunityComment> result = commentRepository
                .findByPost_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
                        postId,
                        PageRequest.of(page, size)
                );

        return new CommentListResponse(
                result.getContent().stream()
                        .map(comment -> toItem(comment, viewerId))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.hasNext()
        );
    }

    CommentListResponse.CommentItem toItem(CommunityComment comment, Long viewerId) {
        return new CommentListResponse.CommentItem(
                comment.getId(),
                authorMapper.toResponse(comment.getAuthor()),
                comment.getContent(),
                viewerId != null && comment.getAuthor().getId().equals(viewerId),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPostQueryException();
        }
    }
}
