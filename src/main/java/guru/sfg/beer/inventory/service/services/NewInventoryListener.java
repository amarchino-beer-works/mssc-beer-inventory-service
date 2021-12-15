package guru.sfg.beer.inventory.service.services;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.events.NewInventoryEvent;
import guru.sfg.brewery.util.JmsQueues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewInventoryListener {

	private final BeerInventoryRepository beerInventoryRepository;
	
	@JmsListener(destination = JmsQueues.NEW_INVENTORY_QUEUE)
	public void listen(NewInventoryEvent event) {
		log.debug("Got inventory: " + event);
		BeerInventory beerInventory = beerInventoryRepository.findByUpc(event.getBeerDto().getUpc())
			.orElse(BeerInventory.builder()
					.beerId(event.getBeerDto().getId())
					.upc(event.getBeerDto().getUpc())
					.build());
		beerInventory.setQuantityOnHand(event.getBeerDto().getQuantityOnHand());
		beerInventoryRepository.save(beerInventory);
	}
}
