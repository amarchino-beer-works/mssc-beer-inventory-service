package guru.sfg.beer.inventory.service.services;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationServiceImpl implements AllocationService {
	
	private final BeerInventoryRepository beerInventoryRepository;

	@Override
	public Boolean allocateOrder(BeerOrderDto beerOrderDto) {
		log.debug("Allocating OrderId: " + beerOrderDto.getId());
		AtomicInteger totalOrdered = new AtomicInteger();
		AtomicInteger totalAllocated = new AtomicInteger();
		
		beerOrderDto.getBeerOrderLines().forEach(beerOrderLine -> {
			int orderQuantity = beerOrderLine.getOrderQuantity() != null ? beerOrderLine.getOrderQuantity() : 0;
			int allocatedQuantity = beerOrderLine.getQuantityAllocated() != null ? beerOrderLine.getQuantityAllocated() : 0;
			if(orderQuantity > allocatedQuantity) {
				allocateBeerOrderLine(beerOrderLine);
			}
			totalOrdered.addAndGet(orderQuantity);
			totalAllocated.addAndGet(allocatedQuantity);
		});
		log.debug("Total ordered: " + totalOrdered.get() + " Total Allocated: " + totalAllocated.get());
		return totalOrdered.get() == totalAllocated.get();
	}

	private void allocateBeerOrderLine(BeerOrderLineDto beerOrderLine) {
		List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByUpc(beerOrderLine.getUpc());
		
		beerInventoryList.forEach(beerInventory -> {
			int inventory = beerInventory.getQuantityOnHand() != null ? beerInventory.getQuantityOnHand() : 0;
			int orderQuantity = beerOrderLine.getOrderQuantity() != null ? beerOrderLine.getOrderQuantity() : 0;
			int allocatedQuantity = beerOrderLine.getQuantityAllocated() != null ? beerOrderLine.getQuantityAllocated() : 0;
			int quantityToAllocate = orderQuantity - allocatedQuantity;
			
			if(inventory >= quantityToAllocate) {
				// Full allocation
				inventory = inventory - quantityToAllocate;
				beerOrderLine.setQuantityAllocated(orderQuantity);
				beerInventory.setQuantityOnHand(inventory);
				beerInventoryRepository.save(beerInventory);
			} else if (inventory > 0) {
				// Partial allocation
				beerOrderLine.setQuantityAllocated(allocatedQuantity + inventory);
				beerInventory.setQuantityOnHand(0);
				beerInventoryRepository.delete(beerInventory);
			}
		});
	}

}
