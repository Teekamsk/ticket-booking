package com.moviebooking.ticket_booking.discount.handler;

import com.moviebooking.ticket_booking.discount.dto.DiscountRequest;
import com.moviebooking.ticket_booking.discount.dto.DiscountResponse;
import com.moviebooking.ticket_booking.discount.mapper.DiscountMapper;
import com.moviebooking.ticket_booking.discount.service.DiscountCommand;
import com.moviebooking.ticket_booking.discount.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscountRequestHandler {

    private final DiscountService discountService;

    public DiscountResponse create(DiscountRequest request) {
        return DiscountMapper.toResponse(discountService.create(toCommand(request)));
    }

    public DiscountResponse update(Long id, DiscountRequest request) {
        return DiscountMapper.toResponse(discountService.update(id, toCommand(request)));
    }

    public void deactivate(Long id) {
        discountService.deactivate(id);
    }

    public List<DiscountResponse> list() {
        return discountService.listAll().stream().map(DiscountMapper::toResponse).toList();
    }

    public DiscountResponse get(Long id) {
        return DiscountMapper.toResponse(discountService.getOrThrow(id));
    }

    private DiscountCommand toCommand(DiscountRequest r) {
        return new DiscountCommand(r.code(), r.discountType(), r.value(), r.maxDiscountAmount(),
                r.minOrderAmount(), r.validFrom(), r.validTo(), r.maxTotalUses(), r.maxUsesPerUser(),
                r.activeOrDefault());
    }
}
