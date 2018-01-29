package milo.te4.services;

import milo.te4.beans.LoginBean;
import milo.te4.beans.RecipeBean;
import milo.te4.utilities.Sort;
import javax.ejb.EJB;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class RecipeService {
    @EJB
    RecipeBean recipeBean;
    @EJB
    LoginBean loginBean;

    @GET
    @Path("recipes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecipes(@QueryParam("sortby") String sort) {
        Sort sortBy;
        // vad som ska sorteras efter från queryparametern
        switch(sort){
            case "upvotes_desc": sortBy = Sort.UPVOTESDESC; break;
            case "upvotes_asc": sortBy = Sort.UPVOTESASC; break;
            case "date_desc": sortBy = Sort.DATEDESC; break;
            case "date_asc": sortBy = Sort.DATEASC; break;
            default: sortBy = Sort.DATEASC; break;
        }
        JsonArray recipes = recipeBean.getRecipes(sortBy);
        if (recipes != null) {
            return Response.ok(recipes).build();
        } else {
            return Response.status(500).build();
        }
    }

    @GET
    @Path("recipe")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecipe(@QueryParam("id") int id) {
        JsonObject recipe = recipeBean.getRecipe(id);
        if (recipe != null) {
            return Response.ok(recipe).build();
        } else {
            return Response.status(500).build();
        }
    }

    @GET
    @Path("login")
    @Produces(MediaType.TEXT_PLAIN)
    public Response login(@Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            return Response.ok("true").build(); //skicka med true om inloggning lyckas
        } else {
            return Response.status(401).entity("false").build(); //skicka med false om misslyckades
        }
    }

    @POST
    @Path("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(String body){
        int statusCode = loginBean.createUser(body);
        return Response.status(statusCode).build();
    }

    @POST
    @Path("recipe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postRecipe(String body, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.postRecipe(body);
            return Response.status(statusCode).build();
        } else{
            return Response.status(401).build();
        }

    }

    @POST
    @Path("comment")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postComment(String body, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.postComment(body);
            return Response.status(statusCode).build();
        } else{
            return Response.status(401).build();
        }

    }

    @POST
    @Path("upvote")
    public Response upvoteRecipe(@QueryParam("recipe") int recipeID, @QueryParam("user") String user, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.upvoteRecipe(recipeID, user);
            return Response.status(statusCode).build();
        } else{
            return Response.status(401).build();
        }
    }

    @PUT
    @Path("recipe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putRecipe(String body, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.putRecipe(body);
            return Response.status(statusCode).build();
        } else{
            return Response.status(401).build();
        }

    }

    @PUT
    @Path("comment")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putComment(String body, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.putComment(body);
            return Response.status(statusCode).build();
        } else{
            return Response.status(401).build();
        }

    }

    @DELETE
    @Path("recipe")
    public Response deleteRecipe(@QueryParam("id") int id, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.deleteRecipe(id);
            return Response.status(statusCode).build();
        }else{
            return Response.status(401).build();
        }

    }

    @DELETE
    @Path("comment")
    public Response deleteComment(@QueryParam("id") int id, @Context HttpHeaders header){
        if(loginBean.checkCredentials(header.getHeaderString("Authorization"))){
            int statusCode = recipeBean.deleteComment(id);
            return Response.status(statusCode).build();
        }else{
            return Response.status(401).build();
        }

    }

}
