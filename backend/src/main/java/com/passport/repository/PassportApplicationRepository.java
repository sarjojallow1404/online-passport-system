
package com.passport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.passport.dto.AdminApplicationListItem;
import com.passport.entity.PassportApplication;

public interface PassportApplicationRepository
        extends JpaRepository<PassportApplication, Long> {

    List<PassportApplication> findAllByApplicantIdOrderByIdDesc(
            Long applicantId);

    boolean existsByApplicantId(Long applicantId);


    Optional<PassportApplication> findByIdAndApplicantId(
            Long id, Long applicantId);

    long countByApplicationStatus(String applicationStatus);

    // Count applications that are not drafts.
    long countByApplicationStatusNot(String applicationStatus);

    @Query(
        value = """
            SELECT new com.passport.dto.AdminApplicationListItem(
                a.id,
                a.applicationNumber,
                a.applicantId,
                u.email,
                p.firstName,
                p.middleName,
                p.lastName,
                a.passportType,
                a.applicationStatus,
                a.submissionDate,
                a.createdAt
            )
            FROM PassportApplication a, User u, ApplicantProfile p
            WHERE u.id = a.applicantId
              AND p.userId = a.applicantId
              AND a.applicationStatus <> 'DRAFT'
              AND (
                  :status IS NULL
                  OR :status = ''
                  OR a.applicationStatus = :status
              )
              AND (
                  :search IS NULL
                  OR :search = ''
                  OR LOWER(a.applicationNumber)
                     LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.email)
                     LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(CONCAT(
                      p.firstName, ' ',
                      COALESCE(p.middleName, ''), ' ',
                      p.lastName
                  )) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY a.id DESC
            """,
        countQuery = """
            SELECT COUNT(a)
            FROM PassportApplication a, User u, ApplicantProfile p
            WHERE u.id = a.applicantId
              AND p.userId = a.applicantId
              AND a.applicationStatus <> 'DRAFT'
              AND (
                  :status IS NULL
                  OR :status = ''
                  OR a.applicationStatus = :status
              )
              AND (
                  :search IS NULL
                  OR :search = ''
                  OR LOWER(a.applicationNumber)
                     LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.email)
                     LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(CONCAT(
                      p.firstName, ' ',
                      COALESCE(p.middleName, ''), ' ',
                      p.lastName
                  )) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """
    )
    Page<AdminApplicationListItem> searchForAdmin(
        @Param("search") String search,
        @Param("status") String status,
        Pageable pageable
    );
}
