package com.note.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.note.constant.ResultCode;
import com.note.constant.SecretConstant;
import com.note.exception.BusinessException;

/**
 * BCrypt 密码加密工具类
 */
public class BcryptUtils {


    public static  String enBcrypt() {
        return BCrypt.hashpw(SecretConstant.SECRET_KEY, BCrypt.gensalt());
    }

    public static boolean checkBcrypt(String password, String hash) {
        if(StrUtil.isBlank(password)) throw new BusinessException(ResultCode.Empty);
        return BCrypt.checkpw(password, hash);
    }
}
