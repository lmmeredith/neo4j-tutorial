package org.neo4j.tutorial;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.neo4j.tutorial.matchers.ContainsSpecificCompanions.contains;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * This Koan will introduce indexing based on the built-in index framework based
 * on Lucene. It'll give you a feeling for the wealth of bad guys the Doctor has
 * faced.
 */
public class Koan3 {

    private DoctorWhoUniverse universe;

    @Before
    public void createDatabase() throws Exception {
        universe = new DoctorWhoUniverse();
    }

    @Test
    public void shouldRetrieveCompanionsIndexFromTheDatabase() {
        Index<Node> companions = null;

        // SNIPPET_START

        companions = universe.getDatabase().index().forNodes("companions");

        // SNIPPET_END

        assertNotNull(companions);
        assertThat(companions, contains("Rose Tyler", "Adam Mitchell", "Jack Harkness", "Mickey Smith", "Donna Noble", "Martha Jones"));
    }

    @Test
    public void addingToAnIndexShouldBeHandledAsAMutatingOperation() {
        Node nixon = createNewCharacterNode("Richard Nixon");

        GraphDatabaseService db = universe.getDatabase();
        // SNIPPET_START

        Transaction tx = db.beginTx();
        try {
            db.index().forNodes("characters").add(nixon, "name", nixon.getProperty("name"));
            tx.success();
        } finally {
            tx.finish();
        }

        // SNIPPET_END

        assertNotNull(db.index().forNodes("characters").get("name", "Richard Nixon").getSingle());
    }

    @Test
    public void shouldFindSpeciesBeginningWithTheLetterSAndEndingWithTheLetterNUsingLuceneQuery() throws Exception {
        IndexHits<Node> species = null;

        // SNIPPET_START

        species = universe.getDatabase().index().forNodes("species").query("species", "S*n");

        // SNIPPET_END

        //assertTrue(containsOnlySontaranSlitheenAndSilurian(indexHits));
       assertThat(species, contains("Silurian", "Slitheen", "Sontaran"));
    }
    
    @Test
    public void shouldKeepDatabaseAndIndexInSyncWhenCyberleaderIsDeleted() throws Exception {
        GraphDatabaseService db = universe.getDatabase();

        Index<Node> enemies = db.index().forNodes("enemies");
        Node cyberleader = enemies.get("name", "Cyberleader").getSingle();

        // SNIPPET_START

        Transaction tx = db.beginTx();
        try {
            enemies.remove(cyberleader);
            for (Relationship rel : cyberleader.getRelationships()) {
                rel.delete();
            }
            cyberleader.delete();
            tx.success();
        } finally {
            tx.finish();
        }

        // SNIPPET_END

        assertNull("Cyberleader has not been deleted from the enemies index.", enemies.get("name", "Cyberleader").getSingle());

        try {
            db.getNodeById(cyberleader.getId());
            fail("Cyberleader has not been deleted from the database.");
        } catch (NotFoundException nfe) {
        }
    }

    private boolean containsOnlySontaranSlitheenAndSilurian(IndexHits<Node> indexHits) {
        boolean foundSilurian = false;
        boolean foundSlitheen = false;
        boolean foundSontaran = false;

        for (Node n : indexHits) {
            String property = (String) n.getProperty("species");

            if (property.equals("Silurian")) {
                foundSilurian = true;
            }
            if (property.equals("Sontaran")) {
                foundSontaran = true;
            }
            if (property.equals("Slitheen")) {
                foundSlitheen = true;
            }

            if (foundSilurian && foundSontaran && foundSlitheen) {
                return true;
            }
        }

        return false;
    }

    private Node createNewCharacterNode(String characterName) {
        Node character = null;
        GraphDatabaseService db = universe.getDatabase();
        Transaction tx = db.beginTx();
        try {
            character = db.createNode();
            character.setProperty("name", characterName);
            tx.success();
        } finally {
            tx.finish();
        }

        return character;
    }
}