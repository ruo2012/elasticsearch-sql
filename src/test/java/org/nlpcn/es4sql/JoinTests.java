package org.nlpcn.es4sql;


import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.plugin.nlpcn.HashJoinElasticExecutor;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import org.junit.Assert;
import org.nlpcn.es4sql.exception.SqlParseException;
import org.nlpcn.es4sql.query.HashJoinElasticRequestBuilder;
import org.nlpcn.es4sql.query.SqlElasticRequestBuilder;
import org.nlpcn.es4sql.query.SqlElasticSearchRequestBuilder;

import java.io.IOException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import static org.nlpcn.es4sql.TestsConstants.TEST_INDEX;

/**
 * Created by Eliran on 22/8/2015.
 */
public class JoinTests {

    @Test
    public void joinParseCheckSelectedFieldsSplit() throws SqlParseException, SQLFeatureNotSupportedException, IOException {
        String query = "SELECT a.firstname ,a.lastname , a.gender ,d.name  FROM elasticsearch-sql_test_index/people a " +
                "LEFT JOIN elasticsearch-sql_test_index/dog d on d.holdersName = a.firstname " +
                " WHERE " +
                " (a.age > 10 OR a.balance > 2000)" +
                " AND d.age > 1";
        SearchHit[] hits = hashJoinGetHits(query);
        Assert.assertEquals(2,hits.length);

        Map<String,Object> oneMatch = ImmutableMap.of("a.firstname", (Object)"Daenerys", "a.lastname","Targaryen",
                                                        "a.gender","M","d.name", "rex");
        Map<String,Object> secondMatch = new HashMap<>();
        secondMatch.put("a.firstname","Hattie");
        secondMatch.put("a.lastname","Bond");
        secondMatch.put("a.gender","M");
        secondMatch.put("d.name","snoopy");

        Assert.assertTrue(hitsContains(hits, oneMatch));
        Assert.assertTrue(hitsContains(hits,secondMatch));

    }

    private SearchHit[] hashJoinGetHits(String query) throws SqlParseException, SQLFeatureNotSupportedException, IOException {
        SearchDao searchDao = MainTestSuite.getSearchDao();
        SqlElasticRequestBuilder explain = searchDao.explain(query);
        HashJoinElasticExecutor executor = new HashJoinElasticExecutor(searchDao.getClient(), (HashJoinElasticRequestBuilder) explain);
        executor.run();
        return executor.getHits().getHits();
    }

    private boolean hitsContains(SearchHit[] hits, Map<String, Object> matchMap) {
        for(SearchHit hit : hits){
            Map<String, Object> hitMap = hit.sourceAsMap();
            boolean matchedHit = true;
            for(Map.Entry<String,Object> entry: hitMap.entrySet()){
                if(!matchMap.containsKey(entry.getKey())) {
                    matchedHit = false;
                    break;
                }
                if(!matchMap.get(entry.getKey()).equals(entry.getValue())){
                    matchedHit = false;
                    break;
                }
            }
            if(matchedHit) return true;
        }
        return false;
    }


    @Test
    public void joinWithNoWhereButWithCondition() throws SQLFeatureNotSupportedException, IOException, SqlParseException {
        String query = String.format("select c.gender , h.name,h.words from %s/gotCharacters c " +
                "JOIN %s/gotHouses h " +
                "on c.house = h.name",TEST_INDEX,TEST_INDEX);
        SearchHit[] hits = hashJoinGetHits(query);
        Assert.assertEquals(4,hits.length);
        Map<String,Object> someMatch =  ImmutableMap.of("c.gender", (Object)"F", "h.name","Targaryen",
                "h.words","fireAndBlood");
        Assert.assertTrue(hitsContains(hits, someMatch));
    }
    @Test
    public void joinNoConditionButWithWhere() throws SQLFeatureNotSupportedException, IOException, SqlParseException {

        String query = String.format("select c.firstname,c.parents.father , h.name,h.words from %s/gotCharacters c " +
                "JOIN %s/gotHouses h " +
                "where c.firstname='Daenerys'",TEST_INDEX,TEST_INDEX);
        SearchHit[] hits = hashJoinGetHits(query);
        Assert.assertEquals(3,hits.length);

    }

    @Test
    public void joinNoConditionAndNoWhere() throws SQLFeatureNotSupportedException, IOException, SqlParseException {

        String query = String.format("select c.firstname,c.parents.father , h.name,h.words from %s/gotCharacters c " +
                "JOIN %s/gotHouses h ",TEST_INDEX,TEST_INDEX);
        SearchHit[] hits = hashJoinGetHits(query);
        Assert.assertEquals(12,hits.length);

    }


}
