package milo.te4.beans;

import milo.te4.utilities.ConnectionFactory;
import milo.te4.utilities.Sort;

import javax.ejb.Stateless;
import javax.json.*;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class RecipeBean {
    // hämta ett recept
    public JsonObject getRecipe(int id) {
        try {
            String sql = "SELECT id, name, recipes.user, date, description, instructions, picture,  portions, COUNT(id) " +
                    "AS upvotes FROM recipes,upvotes WHERE recipes.id = upvotes.recipe AND recipes.id = " + id;
            ResultSet recipeData = getData(sql);

            if (recipeData.next()) {
                JsonObject recipe = Json.createObjectBuilder()
                        .add("id", recipeData.getInt("id"))
                        .add("name", recipeData.getString("name"))
                        .add("user", recipeData.getString("user"))
                        .add("picture", recipeData.getString("picture"))
                        .add("date", recipeData.getDate("date").toString())
                        .add("description", recipeData.getString("description"))
                        .add("instructions", recipeData.getString("instructions"))
                        .add("portions", recipeData.getInt("portions"))
                        .add("ingredients", getRecipeIngredients(id))
                        .add("comments", getRecipeComments(id))
                        .add("tags", getRecipeTags(id))
                        .add("upvotes", recipeData.getInt("upvotes"))
                        .build();
                return recipe;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // hämta alla recept sortera antinge upvotes eller datum
    public JsonArray getRecipes(Sort sort) {
        try {
            String sortString;
            switch (sort) {
                case UPVOTESDESC:
                    sortString = "upvotes DESC";
                    break;
                case UPVOTESASC:
                    sortString = "upvotes ASC";
                    break;
                case DATEDESC:
                    sortString = "date DESC";
                    break;
                case DATEASC:
                    sortString = "date ASC";
                    break;
                default:
                    sortString = "date ASC";
                    break;
            }
            String sql = "SELECT id, name, username AS user, date, picture, description, instructions, portions, COUNT(recipe) " +
                    "AS upvotes FROM (recipes_join_upvotes) GROUP BY id ORDER BY " + sortString;

            ResultSet recipeData = getData(sql);

            JsonArrayBuilder recipes = Json.createArrayBuilder();

            while (recipeData.next()) {
                JsonObject recipe = Json.createObjectBuilder()
                        .add("id", recipeData.getInt("id"))
                        .add("name", recipeData.getString("name"))
                        .add("user", recipeData.getString("user"))
                        .add("picture", recipeData.getString("picture"))
                        .add("date", recipeData.getDate("date").toString())
                        .add("description", recipeData.getString("description"))
                        .add("instructions", recipeData.getString("instructions"))
                        .add("portions", recipeData.getInt("portions"))
                        .add("ingredients", getRecipeIngredients(recipeData.getInt("id")))
                        .add("comments", getRecipeComments(recipeData.getInt("id")))
                        .add("tags", getRecipeTags(recipeData.getInt("id")))
                        .add("upvotes", recipeData.getInt("upvotes"))
                        .build();
                recipes.add(recipe);
            }
            return recipes.build(); //success
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; //error
    }

    // lägga till recept, denna kör även metoden postRecipeIngredients och postRecipeTags
    public int postRecipe(String body) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(body));
            JsonObject recipe = jsonReader.readObject();
            jsonReader.close();

            //int id = recipe.getInt("id");
            String name = recipe.getString("name");
            String user = recipe.getString("user");
            String picture = recipe.getString("picture");
            String description = recipe.getString("description");
            String instructions = recipe.getString("instructions");
            int portions = recipe.getInt("portions");

            JsonArray ingredients = recipe.getJsonArray("ingredients");
            JsonArray tags = recipe.getJsonArray("tags");


            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = String.format("INSERT INTO recipes VALUES(NULL, '%s', '%s', '%s', CURRENT_DATE(), '%s', '%s', '%d')",
                    name, user, picture, description, instructions, portions);

            stmt.executeUpdate(sql);

            //lägga till de saker som är tillhörande till receptet men inte som ett fält
            postRecipeIngredients(ingredients, getLastPostedRecipe());
            postRecipeTags(tags, getLastPostedRecipe());

            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }

    // lägga till en kommentar
    public int postComment(String body) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(body));
            JsonObject comment = jsonReader.readObject();
            jsonReader.close();

            int recipe = comment.getInt("recipe");
            String text = comment.getString("text");
            String user = comment.getString("user");

            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = String.format("INSERT INTO comments VALUES(NULL, %d, CURRENT_DATE(), '%s', '%s')",
                    recipe, user, text);

            stmt.executeUpdate(sql);

            connection.close();

            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }

    // ändra ett recept kör även metoder för ingredienser och taggar som postRecipe
    public int putRecipe(String body) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(body));
            JsonObject recipe = jsonReader.readObject();
            jsonReader.close();

            int id = recipe.getInt("id");
            String name = recipe.getString("name");
            String description = recipe.getString("description");
            String instructions = recipe.getString("instructions");
            int portions = recipe.getInt("portions");

            JsonArray ingredients = recipe.getJsonArray("ingredients");
            JsonArray tags = recipe.getJsonArray("tags");

            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = String.format("UPDATE recipes " +
                            "SET name = '%s', description = '%s', instructions = '%s', portions = %d " +
                            "WHERE id = %d"
                    , name, description, instructions, portions, id);

            stmt.executeUpdate(sql);

            putRecipeIngredients(ingredients, id);
            putRecipeTags(tags, id);

            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }

    // ändra kommentar
    public int putComment(String body) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(body));
            JsonObject comment = jsonReader.readObject();
            jsonReader.close();

            int id = comment.getInt("id");
            String text = comment.getString("comment");

            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = String.format("UPDATE comments SET comment = '%s' WHERE id = %d", text, id);
            stmt.executeUpdate(sql);
            connection.close();

            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }

    // ta bort ett recept
    public int deleteRecipe(int id) {
        try {
            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = "DELETE FROM recipes WHERE id = " + id;
            stmt.executeUpdate(sql);

            connection.close();
            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }

    // ta bort en kommentar
    public int deleteComment(int id) {
        try {
            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = "DELETE FROM comments WHERE id = " + id;
            stmt.executeUpdate(sql);

            connection.close();

            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }

    // upvota ett recept
    public int upvoteRecipe(int recipeID, String user) {
        try {
            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = "SELECT * FROM upvotes WHERE recipe = " + recipeID;
            ResultSet upvotes = stmt.executeQuery(sql);

            //göra en lista med vilka användare som har röstat på detta receptet
            List<String> recipeUpvotes = new ArrayList<>();
            while (upvotes.next()) {
                recipeUpvotes.add(upvotes.getString("user"));
            }

            //bara rösta om användaren inte redan har röstat på receptet
            if (!recipeUpvotes.contains(user)) {
                sql = String.format("INSERT INTO upvotes VALUES('%s', %d)", user, recipeID);
                stmt.executeUpdate(sql);
            } else {
                connection.close();
                return 0;
            }
            connection.close();
            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }


    // metod för att hämta data, kan inte stänga connection då result settet används på andra ställen samtidigt
    private ResultSet getData(String sql) {
        try {
            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet data = stmt.executeQuery(sql);
            return data;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // körs varje gång en ingrediens läggs till
    private void postGrocery(String body) throws SQLException, ClassNotFoundException {
        Connection connection = ConnectionFactory.getConnection();
        Statement stmt = connection.createStatement();
        //hämta först hem alla livsmedel för att kunna kolla om det redan finns i databasen
        String sql = "SELECT name FROM groceries";
        ResultSet data = stmt.executeQuery(sql);

        List<String> groceries = new ArrayList<>();

        while (data.next()) {
            groceries.add(data.getString("name"));
        }
        //om livsmedlet inte finns i databasen ska det läggas till
        if (!groceries.contains(body)) {
            sql = String.format("INSERT INTO groceries VALUES ('%s')", body);
            stmt.executeUpdate(sql);
        }
        connection.close();

    }

    //körs ifrån metoderna som hämtar ett eller flera recept
    private JsonArray getRecipeIngredients(int id) throws SQLException, ClassNotFoundException {
        String sql = "SELECT name, amount, measurement FROM ingredients " +
                "JOIN groceries ON groceries.name = ingredients.grocery " +
                "WHERE recipe = " + id;
        ResultSet ingredientData = getData(sql);

        JsonArrayBuilder ingredients = Json.createArrayBuilder();

        while (ingredientData.next()) {
            JsonObject ingredient = Json.createObjectBuilder()
                    .add("name", ingredientData.getString("name"))
                    .add("amount", ingredientData.getDouble("amount"))
                    .add("measurement", ingredientData.getString("measurement"))
                    .build();
            ingredients.add(ingredient);
        }
        return ingredients.build();
    }

    // --''--
    private JsonArray getRecipeComments(int id) throws SQLException, ClassNotFoundException {
        String sql = "SELECT id, user, date, comment FROM comments WHERE recipe = " + id;
        ResultSet commentData = getData(sql);

        JsonArrayBuilder comments = Json.createArrayBuilder();

        while (commentData.next()) {
            JsonObject comment = Json.createObjectBuilder()
                    .add("id", commentData.getInt("id"))
                    .add("user", commentData.getString("user"))
                    .add("date", commentData.getDate("date").toString())
                    .add("comment", commentData.getString("comment"))
                    .build();
            comments.add(comment);
        }
        return comments.build();
    }

    // --''--
    private JsonArray getRecipeTags(int id) throws SQLException, ClassNotFoundException {
        String sql = "SELECT name FROM tags WHERE recipe = " + id;
        ResultSet tagData = getData(sql);

        JsonArrayBuilder tags = Json.createArrayBuilder();


        while (tagData.next()) {
            JsonObject tag = Json.createObjectBuilder()
                    .add("name", tagData.getString("name"))
                    .build();
            tags.add(tag);
        }

        return tags.build();
    }

    // hämta det recept som lades upp senast för att kunna lägga till ingredienser och taggar till det receptet
    // då de ligger i andra tabeller
    private int getLastPostedRecipe() throws SQLException, ClassNotFoundException {
        int id = 0;
        String sql = "SELECT id FROM recipes ORDER BY id DESC LIMIT 1";
        ResultSet data = getData(sql);

        if (data.next())
            id = data.getInt("id");

        return id;
    }

    // lägga till ingredienserna till ett recept
    private void postRecipeIngredients(JsonArray ingredients, int recipeID) throws SQLException, ClassNotFoundException {
        Connection connection = ConnectionFactory.getConnection();
        Statement stmt = connection.createStatement();
        String sql = "";

        for (int i = 0; i < ingredients.size(); i++) {
            postGrocery(ingredients.getJsonObject(i).getString("name"));
            String grocery = ingredients.getJsonObject(i).getString("name");
            int amount = ingredients.getJsonObject(i).getInt("amount");
            String measurement = ingredients.getJsonObject(i).getString("measurement");

            sql = String.format("INSERT INTO ingredients VALUES(NULL, %d, '%s', %d, '%s')"
                    , recipeID, grocery, amount, measurement);

            stmt.executeUpdate(sql);
        }

        connection.close();
    }

    // --''--
    private void postRecipeTags(JsonArray tags, int recipeID) throws SQLException, ClassNotFoundException {
        Connection connection = ConnectionFactory.getConnection();
        Statement stmt = connection.createStatement();
        String sql = "";

        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.getJsonObject(i).getString("name");

            sql = String.format("INSERT INTO tags VALUES('%s', %d, NULL)", tag, recipeID);

            stmt.executeUpdate(sql);
        }

        connection.close();
    }

    // ändra ingredienserna i ett recept
    private void putRecipeIngredients(JsonArray ingredients, int recipeID) throws SQLException, ClassNotFoundException {
        Connection connection = ConnectionFactory.getConnection();
        Statement stmt = connection.createStatement();
        //ta bort alla nuvarande ingredienser för att sedan kunna lägga upp de nya
        String sql = "DELETE FROM ingredients WHERE recipe = " + recipeID;
        stmt.executeUpdate(sql);

        for (int i = 0; i < ingredients.size(); i++) {
            postGrocery(ingredients.getJsonObject(i).getString("name"));
            String grocery = ingredients.getJsonObject(i).getString("name");
            int amount = ingredients.getJsonObject(i).getInt("amount");
            String measurement = ingredients.getJsonObject(i).getString("measurement");

            sql = String.format("INSERT INTO ingredients VALUES(NULL, %d, '%s', %d, '%s')"
                    , recipeID, grocery, amount, measurement);

            stmt.executeUpdate(sql);
        }

        connection.close();
    }

    //ändra taggarna på ett recept
    private void putRecipeTags(JsonArray tags, int recipeID) throws SQLException, ClassNotFoundException {
        Connection connection = ConnectionFactory.getConnection();
        Statement stmt = connection.createStatement();
        //samma här ta bort de gamla taggarna för att lägga upp de nya
        String sql = "DELETE FROM tags WHERE recipe = " + recipeID;
        stmt.executeUpdate(sql);

        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.getJsonObject(i).getString("name");

            sql = String.format("INSERT INTO tags VALUES('%s', %d, NULL)", tag, recipeID);

            stmt.executeUpdate(sql);
        }

        connection.close();
    }
}
