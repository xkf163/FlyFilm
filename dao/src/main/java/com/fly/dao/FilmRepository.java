package com.fly.dao;

import com.fly.entity.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by F on 2017/6/27.
 */
@Repository
public interface FilmRepository extends JpaRepository<Film,Long>, QueryDslPredicateExecutor<Film> {
    Film findByDoubanNo(String doubanNo);
    Film findBySubjectContaining(String subject);
}
