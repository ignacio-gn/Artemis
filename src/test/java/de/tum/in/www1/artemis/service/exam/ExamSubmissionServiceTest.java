package de.tum.in.www1.artemis.service.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.Language;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.repository.StudentExamRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

class ExamSubmissionServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "esstest"; // only lower case is supported

    @Autowired
    private ExamSubmissionService examSubmissionService;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudentExamRepository studentExamRepository;

    @Autowired
    private UserRepository userRepository;

    private User student1;

    private Exam exam;

    private Exercise exercise;

    private StudentExam studentExam;

    @BeforeEach
    void init() {
        database.addUsers(TEST_PREFIX, 1, 0, 0, 1);
        student1 = database.getUserByLogin(TEST_PREFIX + "student1");
        exercise = database.addCourseExamExerciseGroupWithOneTextExercise();
        Course course = exercise.getCourseViaExerciseGroupOrCourseMember();
        exam = examRepository.findByCourseId(course.getId()).get(0);
        studentExam = database.addStudentExam(exam);
        studentExam.setWorkingTime(7200); // 2 hours
        studentExam.setUser(student1);
        studentExam.addExercise(exercise);
        studentExam = studentExamRepository.save(studentExam);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testCheckSubmissionAllowance_passIfNonExamSubmission() {
        Course tmpCourse = database.addEmptyCourse();
        Exercise nonExamExercise = ModelFactory.generateTextExercise(ZonedDateTime.now(), ZonedDateTime.now(), ZonedDateTime.now(), tmpCourse);
        // should not throw
        examSubmissionService.checkSubmissionAllowanceElseThrow(nonExamExercise, student1);
        boolean result2 = examSubmissionService.isAllowedToSubmitDuringExam(nonExamExercise, student1, false);
        assertThat(result2).isTrue();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testCheckSubmissionAllowance_isSubmissionInTime() {
        // Should fail when submission is made before start date
        exam.setStartDate(ZonedDateTime.now().plusMinutes(5));
        examRepository.save(exam);
        assertThrows(AccessForbiddenException.class, () -> examSubmissionService.checkSubmissionAllowanceElseThrow(exercise, student1));
        boolean result2 = examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false);
        assertThat(result2).isFalse();
        // Should fail when submission is made after (start date + working time)
        exam.setStartDate(ZonedDateTime.now().minusMinutes(130));
        examRepository.save(exam);
        assertThrows(AccessForbiddenException.class, () -> examSubmissionService.checkSubmissionAllowanceElseThrow(exercise, student1));
        result2 = examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false);
        assertThat(result2).isFalse();
        // Should pass if submission is made in time
        exam.setStartDate(ZonedDateTime.now().minusMinutes(90));
        examRepository.save(exam);
        // should not throw
        examSubmissionService.checkSubmissionAllowanceElseThrow(exercise, student1);
        result2 = examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false);
        assertThat(result2).isTrue();
        // Should fail when submission is made after end date (if no working time is set)
        studentExam.setWorkingTime(0);
        studentExamRepository.save(studentExam);
        exam.setStartDate(ZonedDateTime.now().minusMinutes(130));
        exam.setEndDate(ZonedDateTime.now().minusMinutes(120));
        examRepository.save(exam);
        assertThrows(AccessForbiddenException.class, () -> examSubmissionService.checkSubmissionAllowanceElseThrow(exercise, student1));
        result2 = examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false);
        assertThat(result2).isFalse();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testCheckSubmissionAllowance_allowedToSubmitToExercise() {
        // Should fail if there is no student exam for user
        exam.setStartDate(ZonedDateTime.now().minusMinutes(90));
        examRepository.save(exam);
        studentExam.setUser(null);
        studentExamRepository.save(studentExam);
        assertThrows(EntityNotFoundException.class, () -> examSubmissionService.checkSubmissionAllowanceElseThrow(exercise, student1));
        assertThrows(EntityNotFoundException.class, () -> examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false));
        // Should fail if the user's student exam does not have the exercise
        studentExam.setUser(student1);
        studentExam.removeExercise(exercise);
        studentExamRepository.save(studentExam);
        assertThrows(AccessForbiddenException.class, () -> examSubmissionService.checkSubmissionAllowanceElseThrow(exercise, student1));
        boolean result2 = examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false);
        assertThat(result2).isFalse();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testCheckSubmissionAllowance_testRun() {
        final var instructor = userRepository.findOneWithGroupsAndAuthoritiesByLogin(TEST_PREFIX + "instructor1").get();
        studentExam.setTestRun(true);
        studentExam.setUser(instructor);
        studentExamRepository.save(studentExam);
        assertThat(examSubmissionService.isAllowedToSubmitDuringExam(exercise, instructor, false)).isTrue();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testCheckSubmissionAllowance_submittedStudentExam() {
        studentExam.setSubmitted(true);
        studentExamRepository.save(studentExam);
        assertThat(examSubmissionService.isAllowedToSubmitDuringExam(exercise, student1, false)).isFalse();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "student1", roles = "USER")
    void testPreventMultipleSubmissions() {
        StudentParticipation participation = database.createAndSaveParticipationForExercise(exercise, TEST_PREFIX + "student1");
        Submission existingSubmission = ModelFactory.generateTextSubmission("The initial submission", Language.ENGLISH, true);
        existingSubmission = database.addSubmission(participation, existingSubmission);
        Submission receivedSubmission = ModelFactory.generateTextSubmission("This is a submission", Language.ENGLISH, true);
        receivedSubmission = examSubmissionService.preventMultipleSubmissions(exercise, receivedSubmission, student1);
        assertThat(receivedSubmission.getId()).isEqualTo(existingSubmission.getId());
    }

}