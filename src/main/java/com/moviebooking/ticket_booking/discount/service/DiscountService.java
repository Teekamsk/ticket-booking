package com.moviebooking.ticket_booking.discount.service;

import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.discount.entity.Discount;
import com.moviebooking.ticket_booking.discount.entity.DiscountType;
import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;
import com.moviebooking.ticket_booking.discount.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin management of PROMO discounts. Application/validation lives in {@code DiscountApplicationService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private static final long MAX_PERCENT = 100;

    private final DiscountRepository discountRepository;

    @Transactional
    public Discount create(DiscountCommand command) {
        if (discountRepository.existsByCodeIgnoreCase(command.code())) {
            throw new ConflictException("Discount code '" + command.code() + "' already exists");
        }
        validate(command);
        Discount discount = new Discount();
        discount.setType(DiscountType.PROMO);
        apply(discount, command);
        Discount saved = discountRepository.save(discount);
        log.info("Created promo discount {} (id={})", saved.getCode(), saved.getId());
        return saved;
    }

    @Transactional
    public Discount update(Long id, DiscountCommand command) {
        Discount discount = getOrThrow(id);
        if (!discount.getCode().equalsIgnoreCase(command.code())
                && discountRepository.existsByCodeIgnoreCase(command.code())) {
            throw new ConflictException("Discount code '" + command.code() + "' already exists");
        }
        validate(command);
        apply(discount, command);
        return discount;
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).setActive(false);
        log.info("Deactivated discount id={}", id);
    }

    @Transactional(readOnly = true)
    public List<Discount> listAll() {
        return discountRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Discount getOrThrow(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount " + id + " not found"));
    }

    private void validate(DiscountCommand command) {
        if (command.discountType() == DiscountValueType.PERCENT
                && (command.value() < 1 || command.value() > MAX_PERCENT)) {
            throw new BusinessRuleException("Percent value must be between 1 and 100");
        }
        if (command.discountType() == DiscountValueType.FLAT && command.value() <= 0) {
            throw new BusinessRuleException("Flat value must be positive");
        }
        if (command.maxDiscountAmount() != null && command.maxDiscountAmount() <= 0) {
            throw new BusinessRuleException("maxDiscountAmount must be positive");
        }
        if (command.minOrderAmount() < 0) {
            throw new BusinessRuleException("minOrderAmount must not be negative");
        }
        if (!command.validTo().isAfter(command.validFrom())) {
            throw new BusinessRuleException("validTo must be after validFrom");
        }
    }

    private void apply(Discount discount, DiscountCommand command) {
        discount.setCode(command.code());
        discount.setDiscountType(command.discountType());
        discount.setValue(command.value());
        discount.setMaxDiscountAmount(command.maxDiscountAmount());
        discount.setMinOrderAmount(command.minOrderAmount());
        discount.setValidFrom(command.validFrom());
        discount.setValidTo(command.validTo());
        discount.setMaxTotalUses(command.maxTotalUses());
        discount.setMaxUsesPerUser(command.maxUsesPerUser());
        discount.setActive(command.active());
    }
}
