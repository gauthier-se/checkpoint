package com.checkpoint.api.services.impl;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.collection.GameInteractionStatusDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameInteractionService;

@Service
@Transactional(readOnly = true)
public class GameInteractionServiceImpl implements GameInteractionService {

    private static final Logger log = LoggerFactory.getLogger(GameInteractionServiceImpl.class);

    private final UserRepository userRepository;
    private final WishRepository wishRepository;
    private final BacklogRepository backlogRepository;
    private final UserGameRepository userGameRepository;
    private final UserGamePlayRepository userGamePlayRepository;
    private final RateRepository rateRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;

    public GameInteractionServiceImpl(
            UserRepository userRepository,
            WishRepository wishRepository,
            BacklogRepository backlogRepository,
            UserGameRepository userGameRepository,
            UserGamePlayRepository userGamePlayRepository,
            RateRepository rateRepository,
            ReviewRepository reviewRepository,
            LikeRepository likeRepository) {
        this.userRepository = userRepository;
        this.wishRepository = wishRepository;
        this.backlogRepository = backlogRepository;
        this.userGameRepository = userGameRepository;
        this.userGamePlayRepository = userGamePlayRepository;
        this.rateRepository = rateRepository;
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
    }

    @Override
    public GameInteractionStatusDto getGameInteractionStatus(String userEmail, UUID videoGameId) {
        log.debug("Fetching game interaction status for user {} and game {}", userEmail, videoGameId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        Optional<Wish> wishOpt = wishRepository.findByUserIdAndVideoGameId(user.getId(), videoGameId);
        boolean inWishlist = wishOpt.isPresent();
        Priority wishlistPriority = wishOpt.map(Wish::getPriority).orElse(null);

        Optional<Backlog> backlogOpt = backlogRepository.findByUserIdAndVideoGameId(user.getId(), videoGameId);
        boolean inBacklog = backlogOpt.isPresent();
        Priority backlogPriority = backlogOpt.map(Backlog::getPriority).orElse(null);

        Optional<UserGame> userGameOpt = userGameRepository.findByUserIdAndVideoGameId(user.getId(), videoGameId);
        boolean inLibrary = userGameOpt.isPresent();
        PlayStatus libraryStatus = userGameOpt.map(UserGame::getStatus).orElse(null);
        String libraryNotes = userGameOpt.map(UserGame::getNotes).orElse(null);

        int playCount = (int) userGamePlayRepository.countByUserIdAndVideoGameId(user.getId(), videoGameId);

        Optional<Rate> rateOpt = rateRepository.findByUserPseudoAndVideoGameId(user.getPseudo(), videoGameId);
        Integer userRating = rateOpt.map(Rate::getScore).orElse(null);

        boolean hasReview = reviewRepository.existsByUserPseudoAndVideoGameId(user.getPseudo(), videoGameId);

        Optional<UserGamePlay> mostRecentScoredPlay = userGamePlayRepository
                .findMostRecentScoredPlay(user.getId(), videoGameId);
        Integer lastPlayRating = mostRecentScoredPlay.map(UserGamePlay::getScore).orElse(null);

        boolean liked = likeRepository.existsByUserIdAndVideoGameId(user.getId(), videoGameId);

        return new GameInteractionStatusDto(
                inWishlist,
                wishlistPriority,
                inBacklog,
                backlogPriority,
                inLibrary,
                libraryStatus,
                libraryNotes,
                playCount,
                userRating,
                hasReview,
                lastPlayRating,
                liked
        );
    }
}
