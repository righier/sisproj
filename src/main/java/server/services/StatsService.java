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
import beans.ValueWrapper;
import server.Houses;

@Path("stats")
public class StatsService {
	static final String json = "application/json";

	@GET
	@Produces({json})
	public Response getAll() {
		List<Measurement> list = Houses.getAllMeasurements();
		if (list != null) {
			return Response.ok(list).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("condo/{n}")
	@Produces({json})
	public Response getCondoMeasurements(@PathParam("n") int n) {
		List<Measurement> list = Houses.getCondoMeasurements(n);
		if (list != null) {
			return Response.ok(list).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("house/{id}/{n}")
	@Produces({json})
	public Response getHouseMeasurements(@PathParam("id") String id, @PathParam("n") int n) {
		List<Measurement> list = Houses.getMeasurements(id, n);
		if (list != null) {
			return Response.ok(list).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@POST
	@Path("add")
	@Consumes({json})
	@Produces({json})
	public Response addMeasurements(List<Measurement> measurements) {
		Houses.addMeasurements(measurements);
		return Response.ok().build();
	}
	
	@GET
	@Path("avg/{id}/{n}")
	@Produces({json})
	public Response getHouseAverage(@PathParam("id") String id, @PathParam("n") int n) {
		double value = Houses.getAverage(id, n);
		return Response.ok(new ValueWrapper(value)).build();
	}
	
	@GET
	@Path("sigma/{id}/{n}")
	@Produces({json})
	public Response getHouseSigma(@PathParam("id") String id, @PathParam("n") int n) {
		double value = Houses.getSigma(id, n);
		return Response.ok(new ValueWrapper(value)).build();
	}
	
	@GET
	@Path("condoavg/{n}")
	@Produces({json})
	public Response getCondoAverage(@PathParam("n") int n) {
		double value = Houses.getAverage("", n);
		return Response.ok(new ValueWrapper(value)).build();
	}
	
	@GET
	@Path("condosigma/{n}")
	@Produces({json})
	public Response getCondoSigma(@PathParam("n") int n) {
		double value = Houses.getSigma("", n);
		return Response.ok(new ValueWrapper(value)).build();
	}
}
