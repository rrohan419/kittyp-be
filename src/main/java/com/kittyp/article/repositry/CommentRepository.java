/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.repositry;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.article.entity.Comment;

/**
 * @author rrohan419@gmail.com 
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

}
