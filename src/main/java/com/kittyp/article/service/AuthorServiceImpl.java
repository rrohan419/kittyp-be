package com.kittyp.article.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.kittyp.article.dao.AuthorDao;
import com.kittyp.article.dto.AuthorDto;
import com.kittyp.article.entity.Author;
import com.kittyp.article.model.AuthorModel;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final Mapper mapper;
    private final AuthorDao authorDao;

    @Override
    public PaginationModel<AuthorModel> getAllAuthors(int page, int size) {

        Sort sort = Sort.by(Direction.DESC, KeyConstant.UPDATED_AT);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Author> authorsPage = authorDao.getAllAuthors(pageable);

        List<AuthorModel> authorListModels = authorsPage.getContent().stream()
                .map(author -> mapper.convert(author, AuthorModel.class)).toList();

        return authorPageToModel(new PageImpl<>(authorListModels, pageable, authorsPage.getTotalElements()));
    }

    /**
     * 
     * @param authorPage
     * @return
     */
    private PaginationModel<AuthorModel> authorPageToModel(Page<AuthorModel> authorPage) {
        PaginationModel<AuthorModel> projectModel = new PaginationModel<>();
        projectModel.setModels(authorPage.getContent());
        projectModel.setIsFirst(authorPage.isFirst());
        projectModel.setIsLast(authorPage.isLast());
        projectModel.setTotalElements(authorPage.getTotalElements());
        projectModel.setTotalPages(authorPage.getTotalPages());
        return projectModel;
    }

    @Override
    public AuthorModel saveAuthor(AuthorDto authorDto) {
        return mapper.convert(authorDao.saveAuthor(mapper.convert(authorDto, Author.class)), AuthorModel.class);
    }

    @Override
    public AuthorModel editAuthorById(AuthorDto authorDto, Long id) {
        Author existingAuthor = authorDao.authorById(id);
        existingAuthor.setName(authorDto.getName());
        existingAuthor.setAvatar(authorDto.getAvatar());
        existingAuthor.setRole(authorDto.getRole());
        Author updatedAuthor = authorDao.saveAuthor(existingAuthor);
        return mapper.convert(updatedAuthor, AuthorModel.class);
    }

    

}
