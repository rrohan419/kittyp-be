/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.emailsender;

import com.kittyp.email.dto.IEmailDto;

/**
 * @author rrohan419@gmail.com 
 */
@FunctionalInterface
public interface IEmailSender<T extends IEmailDto, R> {

	R sendEmail(T emailDto);
}
