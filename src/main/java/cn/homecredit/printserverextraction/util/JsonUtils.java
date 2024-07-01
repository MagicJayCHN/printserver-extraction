package cn.homecredit.printserverextraction.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtils {
    /**
     * javaBean,list,array convert to json string
     *
     * @param obj
     * @return
     */
    public static String obj2json(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        String result = null;
        try {
            result = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("[obj2json] error:", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * json string convert to javaBean
     *
     * @param jsonStr
     * @param clazz
     * @return
     */
    public static <T> T json2pojo(String jsonStr, Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        T result = null;
        try {
            if (clazz.newInstance() instanceof String) {
                result = (T) jsonStr;
            } else {
                result = objectMapper.readValue(jsonStr, clazz);
            }
        } catch (Exception e) {
            log.error("[json2pojo] jsonStr:{}, className:{} ,error:", jsonStr, clazz.getSimpleName(), e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * json string convert to map
     *
     * @param jsonStr
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> json2map(String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> result = null;
        try {
            result = objectMapper.readValue(jsonStr, Map.class);
        } catch (IOException e) {
            log.error("[json2map] jsonStr:{}, error:", jsonStr, e);
            throw new RuntimeException(e);
        }
        return result;
    }



    /**
     * json array string convert to list with javaBean
     *
     * @param jsonArrayStr
     * @param clazz
     * @return
     */
    public static <T> List<T> json2list(String jsonArrayStr, Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<T> result = new ArrayList<>();
        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<Map<String, Object>> list = (List<Map<String, Object>>) objectMapper.readValue(jsonArrayStr, new TypeReference<List<T>>() {
            });

            for (Map<String, Object> map : list) {
                result.add(map2pojo(map, clazz));
            }

        } catch (IOException e) {
            log.error("[json2list] IO error:", e);
            throw new RuntimeException(e);
        }

        return result;
    }

    /**
     * map convert to javaBean
     */
    @SuppressWarnings("rawtypes")
    public static <T> T map2pojo(Map map, Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(map, clazz);
    }


    public static Map<String, Object> objectJson2Map(Object object) {
        String jsonStr = obj2json(object);
        return json2map(jsonStr);
    }

}