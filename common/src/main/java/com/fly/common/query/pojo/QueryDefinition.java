package com.fly.common.query.pojo;


import com.fly.common.query.entity.Query;
import com.fly.common.query.entity.QueryContext;
import com.fly.common.utils.ConfigurationUtil;
import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@WebListener
public class QueryDefinition implements ServletContextListener {

    public static final String DEFAULT_CONFIG_LOCATION = "query/*.xml";

    static final Logger log = Logger.getLogger(QueryDefinition.class);

    private static QueryDefinition instance = new QueryDefinition();

    private static final Map querys = new HashMap();

    private final Map cachedFiles = new HashMap();

    private int cachedFilesCount;

    private QueryDefinition() {


    }

    public static QueryDefinition getInstance() {
        return instance;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        System.out.println("...........监听器开始启动.............");
        cachedFilesCount = 0;
        Resource resources[] = ConfigurationUtil.getAllResources(DEFAULT_CONFIG_LOCATION);
        if (resources != null) {
            for (int i = 0; i < resources.length; i++) {
                Resource resource = resources[i];
                log.info("Loading query from {" + resource.toString() + "}");
                try {
                    QueryContext queryContext = (QueryContext) ConfigurationUtil.parseXMLObject(
                            com.fly.common.query.entity.QueryContext.class, resource);
                    List list = queryContext.getQueries();
                    Iterator it = list.iterator();
                    do {
                        if (!it.hasNext())
                            break;
                        Query query = (Query) it.next();
                        Query previous = (Query) querys.put(query.getId(), query);
                        if (previous != null)
                            log.error("Duplicated Query register! id[{" + query.getId() + "}], in file {" + resource.toString() + "}");
                    } while (true);
                    if (resource.getURL().getProtocol().equals("file"))
                        cachedFiles.put(resource, Long.valueOf(resource.getFile().lastModified()));

                    System.out.println("-----contextInitialized.querys-------");
                    System.out.println(querys);

                } catch (IOException e) {
                    log.error("Could not load query from {" + resource.toString() + "}, reason:", e);
                } catch (RuntimeException e) {
                    log.error("Fail to digester query from {" + resource + "}, reason:", e);
                }
            }

            cachedFilesCount = cachedFiles.size();
            log.debug("cached query files: {" + cachedFilesCount + "}");
        }
    }

    public static Query getQueryById(String queryId) {

        // TODO 正式发布时，请注释该行代码
        //instance.update();
        System.out.println("-----getQueryById.querys-------");
        System.out.println(querys);
        return (Query) querys.get(queryId);
    }

    public static Map getQuerys() {

        return instance.querys;
    }



    public void update() {

        if (cachedFilesCount > 0) {
            for (Iterator i = cachedFiles.keySet().iterator(); i.hasNext();) {
                Resource resource = (Resource) i.next();
                synchronized (cachedFiles) {
                    try {
                        if (resource.getFile().lastModified() > ((Long) cachedFiles.get(resource)).longValue()) {
                            QueryContext queryContext = (QueryContext) ConfigurationUtil.parseXMLObject(
                                    com.fly.common.query.entity.QueryContext.class, resource);
                            List list = queryContext.getQueries();
                            Query query;
                            for (Iterator it = list.iterator(); it.hasNext(); log.debug("Update Query id[" + query.getId() + "], in {"
                                    + resource.toString() + "}")) {
                                query = (Query) it.next();
                                instance.querys.put(query.getId(), query);
                            }
                            cachedFiles.put(resource, Long.valueOf(resource.getFile().lastModified()));
                        }
                    } catch (IOException e) {
                        log.error("Could not load query from {" + resource.toString() + "}, reason:", e);
                    } catch (RuntimeException e) {
                        log.error("Fail to digester query from {" + resource.toString() + "}, reason:", e);
                    }
                }
            }

        }
    }



    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
