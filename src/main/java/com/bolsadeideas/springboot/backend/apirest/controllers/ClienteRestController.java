package com.bolsadeideas.springboot.backend.apirest.controllers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bolsadeideas.springboot.backend.apirest.models.entity.Cliente;
import com.bolsadeideas.springboot.backend.apirest.models.services.IClienteService;

@CrossOrigin(origins = { "http://localhost:4200" })
@RestController
@RequestMapping("/api")
public class ClienteRestController {

	@Autowired
	private IClienteService clienteService;
	
	private final Logger log = LoggerFactory.getLogger(ClienteRestController.class);

	@GetMapping("/clientes")
	public List<Cliente> index() {
		return clienteService.findAll();
	}

	@GetMapping("/clientes/page/{page}")
	public Page<Cliente> index(@PathVariable Integer page) {
		Pageable pageable = PageRequest.of(page, 4);
		return clienteService.findAll(pageable);
	}

	@GetMapping("/clientes/{id}")
	public ResponseEntity<?> show(@PathVariable Long id) {
		Cliente cliente = null;
		Map<String, Object> response = new HashMap<>();
		try {
			cliente = clienteService.findById(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error al realizar la consulta en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (cliente == null) {
			response.put("mensaje",
					"El cliente ID: ".concat(id.toString().concat(" no existe en nuestra base de datos")));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Cliente>(cliente, HttpStatus.OK);
	}

	@PostMapping("/clientes")
	public ResponseEntity<?> create( @RequestBody Cliente cliente) {
		Cliente clienteNew = null;
		Map<String, Object> response = new HashMap<>();

		try {
			clienteNew = clienteService.save(cliente);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error al realizar el insert en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put("mensaje", "El cliente ha sido creado con exito");
		response.put("cliente", clienteNew);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	
	
	@PutMapping("/clientes/{id}")
	public ResponseEntity<?> update(@RequestBody Cliente cliente, @PathVariable Long id) {
		Cliente currentCliente = this.clienteService.findById(id);
		Cliente clienteUpdated = null;
		
		Map<String, Object> response = new HashMap<>();
		
		
		if(currentCliente == null) {
			response.put("mensaje", "Error: no se pudo editar, El cliente ID: ".concat(id.toString().concat(" no existe en nuestra base de datos")));
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		try {
		currentCliente.setNombre(cliente.getNombre());
		currentCliente.setApellido(cliente.getApellido());
		currentCliente.setEmail(cliente.getEmail());
		currentCliente.setCreateAt(cliente.getCreateAt());
		
		clienteUpdated = clienteService.save(currentCliente);
}
		catch(DataAccessException e) {
			response.put("mensaje", "Error al actualizar en la base de datos");
			response.put("error",e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "El cliente ha sido actualizado con exito");
		response.put("cliente", clienteUpdated);
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
@DeleteMapping("/clientes/{id}")
public ResponseEntity<?> delete(@PathVariable Long id) {
	Map<String, Object> response = new HashMap<>();
	try {
		Cliente cliente = clienteService.findById(id);
		String nombreFotoAnterior = cliente.getFoto();
		
		if(nombreFotoAnterior != null && nombreFotoAnterior.length() >0) {
			Path rutaFotoAnterior = Paths.get("uploads").resolve(nombreFotoAnterior).toAbsolutePath();
			File archivoFotoAnterior = rutaFotoAnterior.toFile();
			if(archivoFotoAnterior.exists() && archivoFotoAnterior.canRead()) {
				archivoFotoAnterior.delete();
			}
		}
		
	Cliente currentCliente = this.clienteService.findById(id);
	this.clienteService.delete(currentCliente);}
	catch(DataAccessException e) {
		response.put("mensaje", "Error al eliminar en la base de datos");
		response.put("error",e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
		return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	response.put("mensaje", "El cliente eliminado con exito");
	return new ResponseEntity<Map<String,Object>>(response,HttpStatus.OK);
	
}


@PostMapping("/clientes/upload")
public ResponseEntity<?> upload(@RequestParam("archivo") MultipartFile archivo, @RequestParam("id") Long id){
	Map<String, Object> response = new HashMap<>();
	Cliente cliente = clienteService.findById(id);
	
	if(!archivo.isEmpty()) {
		String nombreArchivo = UUID.randomUUID().toString() +"_" + archivo.getOriginalFilename().replace(" ","");
		Path rutaArchivo = Paths.get("uploads").resolve(nombreArchivo).toAbsolutePath();
	
		log.info(rutaArchivo.toString());
		
		try {
		Files.copy(archivo.getInputStream(), rutaArchivo);
	} catch (IOException e) {
		response.put("mensaje", "Error al subir la imagen " + nombreArchivo);
		response.put("error",e.getMessage().concat(": ").concat(e.getCause().getMessage()));
		return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		
	}
	
	String nombreFotoAnterior = cliente.getFoto();
	
	if(nombreFotoAnterior != null && nombreFotoAnterior.length() >0) {
		Path rutaFotoAnterior = Paths.get("uploads").resolve(nombreFotoAnterior).toAbsolutePath();
		File archivoFotoAnterior = rutaFotoAnterior.toFile();
		if(archivoFotoAnterior.exists() && archivoFotoAnterior.canRead()) {
			archivoFotoAnterior.delete();
		}
	}
	cliente.setFoto(nombreArchivo);
	clienteService.save(cliente);
	response.put("cliente", cliente);
	response.put("mensaje","Has subido correctamente la imagen: " + nombreArchivo);
	}
	
	return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
}

@GetMapping("/uploads/img/{nombreFoto:.+}")
public ResponseEntity<Resource> verFoto(@PathVariable String nombreFoto){
	Path rutaArchivo = Paths.get("uploads").resolve(nombreFoto).toAbsolutePath();
	log.info(rutaArchivo.toString());
	Resource recurso = null;
	try {
		recurso = new UrlResource(rutaArchivo.toUri());
	} catch (MalformedURLException e) {
		e.printStackTrace();
	}
	
	if(!recurso.exists() && !recurso.isReadable()) {
		throw new RuntimeException("Error no se pudo cargar la imagen: "+ nombreFoto);
	}
	
	HttpHeaders cabecera = new HttpHeaders();
	cabecera.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"");
	return new ResponseEntity<Resource>(recurso, HttpStatus.OK);
}
 

}