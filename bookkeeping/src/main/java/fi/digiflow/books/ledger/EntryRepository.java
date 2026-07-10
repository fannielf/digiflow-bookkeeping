package fi.digiflow.books.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByDateBetweenOrderByDateAscVoucherNumberAsc(LocalDate start, LocalDate end);

    @Query("select coalesce(max(e.voucherNumber), 0) from Entry e where e.date between :start and :end")
    int findMaxVoucherNumber(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
