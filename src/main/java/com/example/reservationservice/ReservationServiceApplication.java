package com.example.reservationservice;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

@EnableCouchbaseRepositories
@SpringBootApplication
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

/**
	* create primary index on reservations ;
	*/
@Log4j2
@Component
class DataInitializer {

	private final ReservationRepository reservationRepository;

	DataInitializer(ReservationRepository reservationRepository) {
		this.reservationRepository = reservationRepository;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void listen() throws Exception {

		Flux<Reservation> reservationFlux = Flux
			.just("Laurent", "Josh", "Mario")
			.map(name -> new Reservation(UUID.randomUUID().toString(), name))
			.flatMap(this.reservationRepository::save);

		this.reservationRepository
			.findAll().flatMap(this.reservationRepository::delete)
			.thenMany(reservationFlux)
			.thenMany(this.reservationRepository.findAll())
			.subscribe(log::info);
	}
}

@Configuration
class ILoveCouchbase implements CouchbaseConfigurer {

	@Override
	public CouchbaseEnvironment couchbaseEnvironment() throws Exception {
		return DefaultCouchbaseEnvironment.create();
	}

	@Override
	public Cluster couchbaseCluster() throws Exception {
		CouchbaseCluster localhost = CouchbaseCluster.create(couchbaseEnvironment(), "localhost");
		localhost.authenticate("jlong", "password");
		return localhost;
	}

	@Override
	public ClusterInfo couchbaseClusterInfo() throws Exception {
		return couchbaseCluster().clusterManager("Administrator", "asdasd").info();
	}

	@Override
	public Bucket couchbaseClient() throws Exception {
		return couchbaseCluster().openBucket("reservations");
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
class Reservation {

	@Id
	private String id;
	private String name;
}

@ViewIndexed(designDoc = "reservation", viewName = "all")
interface ReservationRepository extends ReactiveCouchbaseRepository<Reservation, String> {
}