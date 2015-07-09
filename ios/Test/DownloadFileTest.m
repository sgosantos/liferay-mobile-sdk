/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

#import "BaseTest.h"

#import "LRDLAppService_v62.h"
#import "LRDownloadUtil.h"
#import "TRVSMonitor.h"

/**
 * @author Bruno Farache
 */
@interface DownloadFileTest : BaseTest <LRProgressDelegate>

@property (nonatomic, strong) NSDictionary *entry;
@property (nonatomic, strong) TRVSMonitor *monitor;
@property (nonatomic) NSInteger portalVersion;

@end

@implementation DownloadFileTest : BaseTest

- (BOOL)isCancelled {
	return NO;
}

- (void)onFailure:(NSError *)error {
	XCTFail(@"Error during download %@.", [error localizedDescription]);
	[self.monitor signal];
}

- (void)onFinished {
	[self.monitor signal];
}

- (void)onProgress:(NSData *)data total:(long long)total {
	XCTAssertTrue([NSThread isMainThread]);
	XCTAssertEqual([self.entry[@"size"] longLongValue], total);
	[self.monitor signal];
}

- (void)testDownload {
	self.monitor = [TRVSMonitor monitor];

	NSOutputStream *outputStream = [NSOutputStream outputStreamToMemory];
	[outputStream open];

	[LRDownloadUtil downloadWebDAVFileWithSession:self.session
		portalVersion:self.portalVersion groupFriendlyURL:@"/guest"
		folderPath:@"" fileTitle:self.entry[@"title"] outputStream:outputStream
		progressDelegate:self];

	[self.monitor wait];

	NSData *data = [outputStream
		propertyForKey:NSStreamDataWrittenToMemoryStreamKey];

	[outputStream close];

	XCTAssertEqual([self.entry[@"size"] longLongValue], [data length]);
}

- (void)testGetDownloadURL {
	NSString *expectedURL = @"http://localhost:8080" \
		"/webdav/guest/document_library" \
		"/folder%20with%20spaces" \
		"/file%20%C3%A1%C3%A9%C3%AD%C3%B2%C3%BA%C3%B1.txt";

	NSString *downloadURL = [LRDownloadUtil
		getWebDAVFileURLWithSession:self.session
		portalVersion:self.portalVersion groupFriendlyURL:@"/guest"
		folderPath:@"folder with spaces" fileTitle:@"file áéíòúñ.txt"];

	XCTAssertEqualObjects(expectedURL, downloadURL);
}

- (void)setUp {
	[super setUp];

	LRDLAppService_v62 *service = [[LRDLAppService_v62 alloc]
		initWithSession:self.session];

	long long repositoryId = [self.settings[@"groupId"] longLongValue];
	NSData *bytes = [@"Hello." dataUsingEncoding:NSUTF8StringEncoding];
	NSString *name = @"test.txt";
	NSError *error;

	self.entry = [service addFileEntryWithRepositoryId:repositoryId
		folderId:0 sourceFileName:name mimeType:@"text/plain" title:name
		description:@"" changeLog:@"" bytes:bytes serviceContext:nil
		error:&error];

	self.portalVersion = [self.settings[@"portalVersion"] integerValue];
}

- (void)tearDown {
	LRDLAppService_v62 *service = [[LRDLAppService_v62 alloc]
		initWithSession:self.session];

	long fileEntryId = [self.entry[@"fileEntryId"] longValue];
	NSError *error;

	[service deleteFileEntryWithFileEntryId:fileEntryId error:&error];
}

@end