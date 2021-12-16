package guru.sfg.beer.inventory.service.services.allocation;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import guru.sfg.beer.inventory.service.services.AllocationService;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.brewery.util.JmsQueues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderAllocationListener {

	private final AllocationService allocationService;
	private final JmsTemplate jmsTemplate;

	@JmsListener(destination = JmsQueues.ALLOCATE_ORDER_QUEUE)
	public void listen(AllocateOrderRequest allocateOrderRequest) {
		AllocateOrderResult.AllocateOrderResultBuilder builder = AllocateOrderResult.builder()
				.beerOrderDto(allocateOrderRequest.getBeerOrderDto());
		
		try {
			Boolean allocationResult = allocationService.allocateOrder(allocateOrderRequest.getBeerOrderDto());
			builder.pendingInventory(!allocationResult);
		} catch(Exception e) {
			log.error("Allocation failed for Order Id: " + allocateOrderRequest.getBeerOrderDto().getId());
			builder.allocationError(Boolean.TRUE);
		}
		jmsTemplate.convertAndSend(JmsQueues.ALLOCATE_ORDER_RESPONSE_QUEUE, builder.build());
	}
}
