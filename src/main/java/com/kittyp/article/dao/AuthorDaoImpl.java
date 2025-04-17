/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dao;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.article.entity.Author;
import com.kittyp.article.repositry.AuthorRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Repository
@RequiredArgsConstructor
public class AuthorDaoImpl implements AuthorDao {

	private final AuthorRepository authorRepository;
	private final Environment env;
	
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Author saveAuthor(Author author) {
		try {
			return authorRepository.save(author);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
