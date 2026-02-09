package com.enterprise.openfinance.uc08.domain.port.in;

import com.enterprise.openfinance.uc08.domain.command.SubmitBulkFileCommand;
import com.enterprise.openfinance.uc08.domain.model.BulkFile;
import com.enterprise.openfinance.uc08.domain.model.BulkFileReport;
import com.enterprise.openfinance.uc08.domain.model.BulkUploadResult;
import com.enterprise.openfinance.uc08.domain.query.GetBulkFileReportQuery;
import com.enterprise.openfinance.uc08.domain.query.GetBulkFileStatusQuery;

import java.util.Optional;

public interface BulkPaymentUseCase {

    BulkUploadResult submitFile(SubmitBulkFileCommand command);

    Optional<BulkFile> getFileStatus(GetBulkFileStatusQuery query);

    Optional<BulkFileReport> getFileReport(GetBulkFileReportQuery query);
}
