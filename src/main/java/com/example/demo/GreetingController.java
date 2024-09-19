package com.example.demo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s! (URI is %s) (URI from JNDI is %s)";
    private final AtomicLong counter = new AtomicLong();

    private static final Logger logger = Logger.getLogger(GreetingController.class.getName());

    @Autowired
    private Neo4JConnectionInfo neo4JConnectionInfo;

    public static ArrayList<String> listJndiResources(String jndiResource) {
        ArrayList<String> resources = new ArrayList<>();
        try {
            JndiTemplate jndiTemplate = new JndiTemplate();
            Context ctx = (InitialContext) jndiTemplate.getContext();
            resources.add(ctx.getNameInNamespace());
            NamingEnumeration<NameClassPair> list = ctx.list(jndiResource);
            while (list.hasMore()) {
                NameClassPair nc = list.next();
                resources.add(nc.getName() + " : " + nc.getClassName());
            }
        } catch (Exception e) {
            resources.add("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return resources;
    }

    public static String lookupJndiStringValue(String name){
        try {
            JndiTemplate jndiTemplate = new JndiTemplate();
            return (String) jndiTemplate.lookup(name);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    String dbTest() {
        Logger logger = Logger.getLogger(GreetingController.class.getName());
        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL("127.0.0.1:3306/phoebus_analytics");
        ds.setUser("phoebus");
        ds.setPassword("phoebus-mariadb");
        try {
            logger.info("trying conn");
            Connection conn = ds.getConnection();
            logger.info("conn worked");
            Statement stmt = conn.createStatement();
            logger.info("stmt worked");
            stmt.executeQuery("SELECT value FROM foo");
            logger.info("query worked");
            //return first result as string
            ResultSet set  = stmt.getResultSet();
            StringBuilder res = new StringBuilder();
            while(set.next()){
                res.append(set.getString(1));
            }
            return res.toString();
        } catch (SQLException e) {
            logger.warning("failed to connect to db: "+ e.getMessage());
            return "Fail: " + e.getMessage();
        }
    }

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name, @RequestParam(value="jndiResource", defaultValue = "java:comp/env") String jndiResource) {
        try{
            return new Greeting(counter.incrementAndGet(), String.format(template, name, neo4JConnectionInfo.getUri(), dbTest()));
        } catch (Exception e) {
            Logger.getLogger(GreetingController.class.getName()).severe(e.getMessage());
            return new Greeting(counter.incrementAndGet(), String.format(template, name, neo4JConnectionInfo.getUri(), e.getMessage()));
        }
     }
}