package server.services;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import beans.Measurement;
import server.Houses;

@Path("stats")
public class StatsService {

	@GET
	@Produces({ "application/json" })
	public Response getAll() {
		List<Measurement> list = Houses.getAllMeasurements();
		return Response.ok(list).build();
	}

	@GET
	@Path("all/{n}")
	@Produces({ "application/json" })
	public Response getCondoMeasurements(@PathParam("n") int n) {
		List<Measurement> list = Houses.getCondoMeasurements(n);
		return Response.ok(list).build();
	}

	@Path("house/{id}/{n}")
	@GET
	@Produces({ "application/json" })
	public Response getHouseMeasurements(@PathParam("id") String id, @PathParam("n") int n) {
		return Response.ok().build();
	}

	@POST
	@Path("add")
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public Response addMeasurements(List<Measurement> measurements) {
		int count = Houses.addMeasurements(measurements);
		System.out.println("adding measure: "+measurements.get(0)+" count: "+count);
		return Response.ok().build();
	}
}
