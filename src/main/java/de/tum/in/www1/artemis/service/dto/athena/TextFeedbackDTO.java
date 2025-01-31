package de.tum.in.www1.artemis.service.dto.athena;

import javax.validation.constraints.NotNull;

import de.tum.in.www1.artemis.domain.Feedback;
import de.tum.in.www1.artemis.domain.TextBlock;

/**
 * A DTO representing a Feedback on a TextExercise, for transferring data to Athena and receiving suggestions from Athena
 */
public record TextFeedbackDTO(long id, long exerciseId, long submissionId, String title, String description, double credits, Long structuredGradingInstructionId,
        Integer indexStart, Integer indexEnd) implements FeedbackDTO {

    /**
     * Creates a TextFeedbackDTO from a Feedback object
     *
     * @param exerciseId    the id of the exercise the feedback is given for
     * @param submissionId  the id of the submission the feedback is given for
     * @param feedback      the feedback object
     * @param feedbackBlock the TextBlock that the feedback is on (must be passed because this record cannot fetch it for itself)
     * @return the TextFeedbackDTO
     */
    public static TextFeedbackDTO of(long exerciseId, long submissionId, @NotNull Feedback feedback, TextBlock feedbackBlock) {
        Integer startIndex = feedbackBlock == null ? null : feedbackBlock.getStartIndex();
        Integer endIndex = feedbackBlock == null ? null : feedbackBlock.getEndIndex();
        Long gradingInstructionId = null;
        if (feedback.getGradingInstruction() != null) {
            gradingInstructionId = feedback.getGradingInstruction().getId();
        }
        return new TextFeedbackDTO(feedback.getId(), exerciseId, submissionId, feedback.getText(), feedback.getDetailText(), feedback.getCredits(), gradingInstructionId,
                startIndex, endIndex);
    }
}
