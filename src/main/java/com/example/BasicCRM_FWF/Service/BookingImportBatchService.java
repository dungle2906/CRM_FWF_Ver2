package com.example.BasicCRM_FWF.Service;

import com.example.BasicCRM_FWF.Model.BookingRecord;
import com.example.BasicCRM_FWF.Model.BookingSalesMap;
import com.example.BasicCRM_FWF.Model.SalesTransaction;
import com.example.BasicCRM_FWF.Repository.BookingRecordRepository;
import com.example.BasicCRM_FWF.Repository.BookingSalesMapRepository;
import com.example.BasicCRM_FWF.Repository.SalesTransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingImportBatchService {

    private final BookingRecordRepository bookingRepository;
    private final BookingSalesMapRepository bookingSalesMapRepository;
    private final SalesTransactionRepository salesTransactionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importBatch(List<BookingRecord> batchBookings) {

        bookingRepository.saveAll(batchBookings);
        entityManager.flush(); // ✅ transaction thật

        Set<String> orderCodes = batchBookings.stream()
                .map(BookingRecord::getOrderId)
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (orderCodes.isEmpty()) return;

        Map<String, SalesTransaction> salesMap =
                salesTransactionRepository.findByOrderCodes(new ArrayList<>(orderCodes))
                        .stream()
                        .collect(Collectors.toMap(
                                SalesTransaction::getOrderCode,
                                Function.identity(),
                                (a, b) -> a
                        ));

        if (salesMap.isEmpty()) return;

        List<BookingSalesMap> mappings = new ArrayList<>();

        for (BookingRecord booking : batchBookings) {
            String orderIdRaw = booking.getOrderId();
            if (orderIdRaw == null || orderIdRaw.isBlank()) continue;

            Arrays.stream(orderIdRaw.split(","))
                    .map(String::trim)
                    .filter(salesMap::containsKey)
                    .forEach(code ->
                            mappings.add(new BookingSalesMap(null, booking, salesMap.get(code)))
                    );
        }

        if (!mappings.isEmpty()) {
            bookingSalesMapRepository.saveAll(mappings);
        }
    }
}
