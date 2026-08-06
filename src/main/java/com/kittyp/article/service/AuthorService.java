package com.kittyp.article.service;

import com.kittyp.article.dto.AuthorDto;
import com.kittyp.article.model.AuthorModel;
import com.kittyp.common.model.PaginationModel;

public interface AuthorService {
    
    /**
     * 
     * @param page
     * @param size
     * @return
     */
    PaginationModel<AuthorModel> getAllAuthors(int page, int size);

    /**
     * 
     * @param authorDto
     * @return
     */
    AuthorModel saveAuthor(AuthorDto authorDto);

    AuthorModel editAuthorById(AuthorDto authorDto, Long id);

    /** Get or create an Author row linked to the current doctor user. */
    AuthorModel getOrCreateForUser(String userUuid, String displayName, String avatarUrl);
}
