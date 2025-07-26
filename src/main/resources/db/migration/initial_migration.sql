Create table user (
	id       BIGINT AUTO_INCREMENT primary key,
	email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
	name varchar(20) not null,
	profile_img_url varchar(255)
);
Create table folder (
	id bigint auto_increment primary key,
	name varchar(20) not null,
	file_url varchar(255),
	summary text ,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	is_favorite boolean default false,
	user_id BIGINT not null,
	foreign key (user_id) references user(id)
);
Create table flashcard (
	id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
	question text not null,
	answer text not null,
	folder_id bigint not null,
	foreign key (folder_id) references folder(id)
);