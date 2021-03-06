package com.fly.service;

import com.fly.entity.Person;

import java.util.List;
import java.util.Map;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 10:33 2017/10/30
 */
public interface PersonService {

    Map<String, Object> findAll(String reqObj) throws Exception;

    List<String> findAllDouBanNo();

    List<String> findImportWithoutLogoList(String filmNumber);

    void batchInsertAndUpdate(List<Person> persons);

    Person findByDouBanNo(String douBanNo);

    Person findOne(Long id);

    void save(Person person);

    Map getPersonNamesByDoubanNos(String personDoubanNos);

    Map getPersonNamesByPersonIds(String personIds);

    Person findByNameContaining(String name);

    List<String> findAllOfFilm(String subject);
}
