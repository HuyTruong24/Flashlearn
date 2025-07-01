Create table user (
	username varchar(20) primary key not null,
	profile_img_url varchar(255)
);
Create table folder (
	id bigint auto_increment primary key,
	name varchar(20) not null,
	file_url varchar(255),
	summary text ,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	is_favorite boolean default false,
	username varchar(20) not null,
	foreign key (username) references user(username)
);
Create table flashcard (
	id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
	question text not null,
	answer text not null,
	folder_id bigint not null,
	foreign key (folder_id) references folder(id)
);