package de.tum.in.www1.artemis.lecture;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.util.ResourceUtils;

import de.tum.in.www1.artemis.domain.Attachment;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Lecture;
import de.tum.in.www1.artemis.domain.enumeration.AttachmentType;
import de.tum.in.www1.artemis.domain.lecture.AttachmentUnit;
import de.tum.in.www1.artemis.service.FilePathService;
import de.tum.in.www1.artemis.service.FileService;

/**
 * Factory for creating Lectures and related objects.
 */
public class LectureFactory {

    /**
     * Generates a Lecture for the given Course.
     *
     * @param startDate The start date of the Lecture
     * @param endDate   The end date of the Lecture
     * @param course    The Course the Lecture belongs to
     * @return The generated Lecture
     */
    public static Lecture generateLecture(ZonedDateTime startDate, ZonedDateTime endDate, Course course) {
        Lecture lecture = new Lecture();
        lecture.setVisibleDate(startDate);
        lecture.setStartDate(startDate);
        lecture.setDescription("Description");
        lecture.setTitle("Lecture");
        lecture.setEndDate(endDate);
        lecture.setCourse(course);
        return lecture;
    }

    /**
     * Generates an AttachmentUnit with an Attachment. The Attachment can be created with or without a link to an image file.
     *
     * @param withFile True, if the Attachment should link to a file
     * @return The generated AttachmentUnit
     */
    public static AttachmentUnit generateAttachmentUnit(boolean withFile) {
        ZonedDateTime started = ZonedDateTime.now().minusDays(5);
        Attachment attachmentOfAttachmentUnit = withFile ? generateAttachmentWithFile(started) : generateAttachment(started);
        AttachmentUnit attachmentUnit = new AttachmentUnit();
        attachmentUnit.setDescription("Lorem Ipsum");
        attachmentOfAttachmentUnit.setAttachmentUnit(attachmentUnit);
        attachmentUnit.setAttachment(attachmentOfAttachmentUnit);
        return attachmentUnit;
    }

    /**
     * Generates an Attachment with AttachmentType FILE.
     *
     * @param date The optional upload and release date of the Attachment
     * @return The generated Attachment
     */
    public static Attachment generateAttachment(ZonedDateTime date) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.FILE);
        if (date != null) {
            attachment.setReleaseDate(date);
            attachment.setUploadDate(date);
        }
        attachment.setName("TestAttachment");
        attachment.setVersion(1);
        return attachment;
    }

    /**
     * Generates an Attachment with AttachmentType FILE and a link to an image file.
     *
     * @param startDate The optional upload and release date of the Attachment
     * @return The generated Attachment
     */
    public static Attachment generateAttachmentWithFile(ZonedDateTime startDate) {
        Attachment attachment = generateAttachment(startDate);
        String testFileName = "test_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
        try {
            FileUtils.copyFile(ResourceUtils.getFile("classpath:test-data/attachment/placeholder.jpg"), FilePathService.getTempFilePath().resolve(testFileName).toFile());
        }
        catch (IOException ex) {
            fail("Failed while copying test attachment files", ex);
        }
        // Path.toString() uses platform dependant path separators. Since we want to use this as a URL later, we need to replace \ with /.
        attachment.setLink(Path.of(FileService.DEFAULT_FILE_SUBPATH, testFileName).toString().replace('\\', '/'));
        return attachment;
    }
}
