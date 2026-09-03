package com.sebu.backend.bookmark.service;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.professor.repository.ProfessorRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookmarkConcurrencyIntegrationTest {

    @Autowired
    BookmarkService bookmarkService;

    @Autowired
    CollegeRepository collegeRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    ProfessorRepository professorRepository;

    @Autowired
    LaboratoryRepository laboratoryRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void simultaneousBookmarkRequestsSucceedWithOneBookmark() throws Exception {
        TestFixture fixture = createFixture();

        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Callable<Void> addBookmark = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                bookmarkService.add(fixture.userId(), fixture.laboratoryId());
                return null;
            };
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                List<Future<Void>> futures = List.of(
                        executor.submit(addBookmark),
                        executor.submit(addBookmark)
                );

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                futures.get(0).get(10, TimeUnit.SECONDS);
                futures.get(1).get(10, TimeUnit.SECONDS);
            } finally {
                start.countDown();
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }

            Integer bookmarkCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM bookmark
                    WHERE user_id = ?
                      AND laboratory_id = ?
                    """,
                    Integer.class,
                    fixture.userId(),
                    fixture.laboratoryId()
            );

            assertThat(bookmarkCount).isOne();
        } finally {
            deleteFixture(fixture);
        }
    }

    private TestFixture createFixture() {
        String suffix = UUID.randomUUID().toString();
        College college = collegeRepository.save(
                new College("동시 북마크 대학-" + suffix)
        );
        Department department = departmentRepository.save(
                new Department(college, "동시 북마크 학과-" + suffix)
        );
        Professor professor = professorRepository.save(
                new Professor(department, "동시 북마크 교수-" + suffix, null)
        );
        Laboratory laboratory = laboratoryRepository.saveAndFlush(
                new Laboratory(
                        professor,
                        department,
                        "동시 북마크 연구실-" + suffix,
                        null,
                        RecruitmentStatus.RECRUITING
                )
        );
        AppUser user = appUserRepository.saveAndFlush(
                new AppUser("bookmark-" + suffix + "@example.com")
        );

        return new TestFixture(
                college.getId(),
                department.getId(),
                professor.getId(),
                laboratory.getId(),
                user.getId()
        );
    }

    private void deleteFixture(TestFixture fixture) {
        jdbcTemplate.update(
                "DELETE FROM bookmark WHERE user_id = ? AND laboratory_id = ?",
                fixture.userId(),
                fixture.laboratoryId()
        );
        jdbcTemplate.update("DELETE FROM laboratory WHERE id = ?", fixture.laboratoryId());
        jdbcTemplate.update("DELETE FROM professor WHERE id = ?", fixture.professorId());
        jdbcTemplate.update("DELETE FROM department WHERE id = ?", fixture.departmentId());
        jdbcTemplate.update("DELETE FROM college WHERE id = ?", fixture.collegeId());
        jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", fixture.userId());
    }

    private record TestFixture(
            Long collegeId,
            Long departmentId,
            Long professorId,
            Long laboratoryId,
            Long userId
    ) {
    }
}
