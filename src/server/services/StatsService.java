package server.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import beans.IntegerCount;
import beans.Measurement;
import server.Houses;

@Path("stats")
public class StatsService {

	@GET
	@Path("all/{n}")
	@Produces({ "application/json" })
	public Response getCondoMeasurements(@PathParam("n") int n) {
		Measurement[] list = Houses.instance.getCondoMeasurements(n);
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
	public Response addMeasurements(Measurement[] measurements) {
		int count = Houses.instance.addMeasurements(measurements);
		return Response.ok(new IntegerCount(count)).build();
	}
}
